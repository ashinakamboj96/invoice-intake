package com.zamp.invoice.service;

import com.zamp.invoice.model.entity.ExtractionEvidence;
import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.enums.InvoiceStatus;
import com.zamp.invoice.enums.ReviewActionType;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.model.dto.InvoiceDetailResponse;
import com.zamp.invoice.model.dto.InvoiceListItem;
import com.zamp.invoice.model.dto.InvoiceListResponse;
import com.zamp.invoice.model.dto.LineItemDto;
import com.zamp.invoice.model.dto.ValidationFailureDto;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.repository.ExtractionEvidenceRepository;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.repository.ValidationFailureRepository;
import com.zamp.invoice.util.InvoiceSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.zamp.invoice.model.dto.InvoiceUploadResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read side of the invoice API: creating the initial record on upload, assembling the full
 * detail view, and running the search/list query. Does not touch review state — see
 * {@code ReviewService} for that.
 */
@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final ValidationFailureRepository validationFailureRepository;
    private final ExtractionEvidenceRepository extractionEvidenceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                           InvoiceLineItemRepository invoiceLineItemRepository,
                           ValidationFailureRepository validationFailureRepository,
                           ExtractionEvidenceRepository extractionEvidenceRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.validationFailureRepository = validationFailureRepository;
        this.extractionEvidenceRepository = extractionEvidenceRepository;
    }

    /** Persists a new {@code Invoice} in {@code PROCESSING} status from an uploaded file; extraction itself is kicked off separately by the caller. */
    public Invoice createInvoice(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }

        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .originalFile(bytes)
                .originalFilename(file.getOriginalFilename())
                .uploadedAt(OffsetDateTime.now())
                .status(InvoiceStatus.PROCESSING)
                .build();

        return invoiceRepository.save(invoice);
    }

    /** Assembles the full detail view for one invoice: fields, line items, resolved/unresolved failures, OCR confidence summary, and (for REJECTED invoices) the related duplicate. */
    public InvoiceDetailResponse getInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        List<LineItemDto> lineItems = invoiceLineItemRepository.findByInvoiceId(invoiceId).stream()
                .map(this::toLineItemDto)
                .toList();

        List<ValidationFailure> allFailures = validationFailureRepository.findByInvoiceId(invoiceId);
        // Duplicate failures surface first: confirming one is the only action that can end the
        // review in a terminal (REJECTED) state, so it's the highest-priority decision on the screen.
        List<ValidationFailureDto> unresolvedFailures = allFailures.stream()
                .filter(failure -> !failure.isResolved())
                .sorted(Comparator.comparing((ValidationFailure failure) -> !failure.getRule().contains("DUPLICATE")))
                .map(ValidationFailureDto::from)
                .toList();
        List<ValidationFailureDto> resolvedFailures = allFailures.stream()
                .filter(ValidationFailure::isResolved)
                .map(ValidationFailureDto::from)
                .toList();

        List<ExtractionEvidence> allEvidence = extractionEvidenceRepository.findByInvoiceId(invoiceId);

        Map<String, Double> evidenceSummary = allEvidence.stream()
                .filter(evidence -> evidence.getLineItemId() == null && evidence.getOcrConfidence() != null)
                .collect(Collectors.toMap(evidence -> evidence.getFieldName().name(), evidence -> evidence.getOcrConfidence().doubleValue()));

        Map<UUID, Map<String, Double>> lineItemEvidenceSummary = allEvidence.stream()
                .filter(evidence -> evidence.getLineItemId() != null && evidence.getOcrConfidence() != null)
                .collect(Collectors.groupingBy(ExtractionEvidence::getLineItemId,
                        Collectors.toMap(evidence -> evidence.getFieldName().name(), evidence -> evidence.getOcrConfidence().doubleValue())));

        UUID relatedInvoiceId = allFailures.stream()
                .filter(failure -> failure.getAction() == ReviewActionType.DUPLICATE_CONFIRMED)
                .findFirst()
                .map(failure -> failure.getRelatedInvoice() != null ? failure.getRelatedInvoice().getId() : null)
                .orElse(null);

        return InvoiceDetailResponse.builder()
                .id(invoice.getId())
                .vendorName(invoice.getVendorName())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .currency(invoice.getCurrency())
                .subtotalAmount(invoice.getSubtotalAmount())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .extractionMethod(invoice.getExtractionMethod())
                .uploadedAt(invoice.getUploadedAt())
                .failureMessage(invoice.getFailureMessage())
                .lineItems(lineItems)
                .unresolvedFailures(unresolvedFailures)
                .resolvedFailures(resolvedFailures)
                .evidenceSummary(evidenceSummary)
                .lineItemEvidenceSummary(lineItemEvidenceSummary)
                .relatedInvoiceId(relatedInvoiceId)
                .build();
    }

    private LineItemDto toLineItemDto(InvoiceLineItem lineItem) {
        return LineItemDto.builder()
                .id(lineItem.getId())
                .lineNumber(lineItem.getLineNumber())
                .description(lineItem.getDescription())
                .quantity(lineItem.getQuantity())
                .unitPrice(lineItem.getUnitPrice())
                .amount(lineItem.getAmount())
                .build();
    }

    /** Filters (any/all optional, unset ones simply don't restrict the query — see {@link InvoiceSpecification}), sorted newest-uploaded-first, and paginated. */
    public InvoiceListResponse listInvoices(InvoiceStatus status,
                                             String vendor,
                                             String invoiceNumber,
                                             String currency,
                                             LocalDate dateFrom,
                                             LocalDate dateTo,
                                             BigDecimal amountMin,
                                             BigDecimal amountMax,
                                             int page,
                                             int size) {
        Specification<Invoice> spec = Specification
                .where(InvoiceSpecification.hasStatus(status))
                // PROCESSING invoices have no extracted fields yet, so a default (unfiltered) view
                // would otherwise be full of blank rows; they get their own "currently processing"
                // widget instead (see listProcessingInvoices). An explicit status=PROCESSING filter
                // still shows them here, matching what the filter literally asks for.
                .and(status == null ? InvoiceSpecification.excludeStatus(InvoiceStatus.PROCESSING) : null)
                .and(InvoiceSpecification.vendorContains(vendor))
                .and(InvoiceSpecification.invoiceNumberContains(invoiceNumber))
                .and(InvoiceSpecification.hasCurrency(currency))
                .and(InvoiceSpecification.dateFrom(dateFrom))
                .and(InvoiceSpecification.dateTo(dateTo))
                .and(InvoiceSpecification.amountMin(amountMin))
                .and(InvoiceSpecification.amountMax(amountMax));

        Page<Invoice> resultPage = invoiceRepository.findAll(spec, PageRequest.of(page, size, Sort.by("uploadedAt").descending()));

        List<InvoiceListItem> items = toListItems(resultPage.getContent());

        return InvoiceListResponse.builder()
                .invoices(items)
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .build();
    }

    /** Currencies actually present on at least one invoice, for populating the list page's currency filter. */
    public List<String> listDistinctCurrencies() {
        return invoiceRepository.findDistinctCurrencies();
    }

    /** The most recently uploaded still-processing invoices, for the list page's "currently processing" widget. */
    public List<InvoiceUploadResponse> listProcessingInvoices() {
        return invoiceRepository.findTop10ByStatusOrderByUploadedAtDesc(InvoiceStatus.PROCESSING).stream()
                .map(invoice -> InvoiceUploadResponse.builder()
                        .id(invoice.getId())
                        .status(invoice.getStatus())
                        .originalFilename(invoice.getOriginalFilename())
                        .uploadedAt(invoice.getUploadedAt())
                        .build())
                .toList();
    }

    /** Bulk-counts unresolved failures for a page of invoices in one query, instead of one query per row. */
    private List<InvoiceListItem> toListItems(List<Invoice> invoices) {
        List<UUID> invoiceIds = invoices.stream().map(Invoice::getId).toList();
        Map<UUID, Long> unresolvedCounts = validationFailureRepository.countUnresolvedByInvoiceIdIn(invoiceIds).stream()
                .collect(Collectors.toMap(ValidationFailureRepository.UnresolvedCount::getInvoiceId,
                        ValidationFailureRepository.UnresolvedCount::getFailureCount));

        return invoices.stream()
                .map(invoice -> InvoiceListItem.builder()
                        .id(invoice.getId())
                        .vendorName(invoice.getVendorName())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .invoiceDate(invoice.getInvoiceDate())
                        .totalAmount(invoice.getTotalAmount())
                        .currency(invoice.getCurrency())
                        .status(invoice.getStatus())
                        .uploadedAt(invoice.getUploadedAt())
                        .unresolvedFailureCount(unresolvedCounts.getOrDefault(invoice.getId(), 0L).intValue())
                        .build())
                .toList();
    }
}
