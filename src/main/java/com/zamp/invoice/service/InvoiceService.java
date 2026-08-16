package com.zamp.invoice.service;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.domain.ReviewActionType;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.dto.InvoiceDetailResponse;
import com.zamp.invoice.dto.InvoiceListItem;
import com.zamp.invoice.dto.InvoiceListResponse;
import com.zamp.invoice.dto.LineItemDto;
import com.zamp.invoice.dto.ValidationFailureDto;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public InvoiceDetailResponse getInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        List<LineItemDto> lineItems = invoiceLineItemRepository.findByInvoiceId(invoiceId).stream()
                .map(this::toLineItemDto)
                .toList();

        List<ValidationFailure> allFailures = validationFailureRepository.findByInvoiceId(invoiceId);
        List<ValidationFailureDto> unresolvedFailures = allFailures.stream()
                .filter(failure -> !failure.isResolved())
                .map(ValidationFailureDto::from)
                .toList();
        List<ValidationFailureDto> resolvedFailures = allFailures.stream()
                .filter(ValidationFailure::isResolved)
                .map(ValidationFailureDto::from)
                .toList();

        Map<String, Double> evidenceSummary = extractionEvidenceRepository.findByInvoiceIdAndLineItemIdIsNull(invoiceId).stream()
                .filter(evidence -> evidence.getOcrConfidence() != null)
                .collect(Collectors.toMap(evidence -> evidence.getFieldName().name(), evidence -> evidence.getOcrConfidence().doubleValue()));

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

    public InvoiceListResponse listInvoices(InvoiceStatus status,
                                             String vendor,
                                             LocalDate dateFrom,
                                             LocalDate dateTo,
                                             BigDecimal amountMin,
                                             BigDecimal amountMax,
                                             int page,
                                             int size) {
        Specification<Invoice> spec = Specification
                .where(InvoiceSpecification.hasStatus(status))
                .and(InvoiceSpecification.vendorContains(vendor))
                .and(InvoiceSpecification.dateFrom(dateFrom))
                .and(InvoiceSpecification.dateTo(dateTo))
                .and(InvoiceSpecification.amountMin(amountMin))
                .and(InvoiceSpecification.amountMax(amountMax));

        Page<Invoice> resultPage = invoiceRepository.findAll(spec, PageRequest.of(page, size, Sort.by("uploadedAt").descending()));

        List<InvoiceListItem> items = resultPage.getContent().stream()
                .map(this::toListItem)
                .toList();

        return InvoiceListResponse.builder()
                .invoices(items)
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .build();
    }

    private InvoiceListItem toListItem(Invoice invoice) {
        int unresolvedFailureCount = validationFailureRepository.countByInvoiceIdAndResolvedFalse(invoice.getId());

        return InvoiceListItem.builder()
                .id(invoice.getId())
                .vendorName(invoice.getVendorName())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .uploadedAt(invoice.getUploadedAt())
                .unresolvedFailureCount(unresolvedFailureCount)
                .build();
    }
}
