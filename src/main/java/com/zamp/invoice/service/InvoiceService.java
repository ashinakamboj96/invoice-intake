package com.zamp.invoice.service;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.dto.InvoiceDetailResponse;
import com.zamp.invoice.dto.InvoiceListResponse;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
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
                .build();
    }

    public InvoiceListResponse listInvoices(InvoiceStatus status, int page, int pageSize) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public byte[] downloadOriginalFile(UUID invoiceId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
