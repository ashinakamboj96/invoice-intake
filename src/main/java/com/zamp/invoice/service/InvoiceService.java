package com.zamp.invoice.service;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.dto.InvoiceDetailResponse;
import com.zamp.invoice.dto.InvoiceListResponse;
import com.zamp.invoice.dto.LineItemDto;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceLineItemRepository invoiceLineItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
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
                .collect(Collectors.toList());

        return InvoiceDetailResponse.builder()
                .id(invoice.getId())
                .status(invoice.getStatus())
                .extractionMethod(invoice.getExtractionMethod())
                .originalFilename(invoice.getOriginalFilename())
                .uploadedAt(invoice.getUploadedAt())
                .vendorName(invoice.getVendorName())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .currency(invoice.getCurrency())
                .subtotalAmount(invoice.getSubtotalAmount())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .failureMessage(invoice.getFailureMessage())
                .lineItems(lineItems)
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

    public InvoiceListResponse listInvoices(InvoiceStatus status, int page, int pageSize) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public byte[] downloadOriginalFile(UUID invoiceId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
