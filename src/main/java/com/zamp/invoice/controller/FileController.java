package com.zamp.invoice.controller;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@RestController
public class FileController {

    private final InvoiceRepository invoiceRepository;

    public FileController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/invoices/{id}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));

        String contentType = invoice.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? "application/pdf" : "image/jpeg";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + invoice.getOriginalFilename() + "\"")
                .body(invoice.getOriginalFile());
    }
}
