package com.zamp.invoice.controller;

import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.dto.InvoiceUploadResponse;
import com.zamp.invoice.exception.UnsupportedFileTypeException;
import com.zamp.invoice.service.ExtractionPipelineService;
import com.zamp.invoice.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/** Handles invoice upload; the extraction pipeline it kicks off runs asynchronously, so this returns as soon as the file is persisted. */
@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/tiff"
    );

    private final InvoiceService invoiceService;
    private final ExtractionPipelineService extractionPipelineService;

    public InvoiceController(InvoiceService invoiceService, ExtractionPipelineService extractionPipelineService) {
        this.invoiceService = invoiceService;
        this.extractionPipelineService = extractionPipelineService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<InvoiceUploadResponse> uploadInvoice(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new UnsupportedFileTypeException("Unsupported file type: " + file.getContentType());
        }

        Invoice invoice = invoiceService.createInvoice(file);
        extractionPipelineService.process(invoice.getId(), invoice.getOriginalFile(), invoice.getOriginalFilename());

        InvoiceUploadResponse response = InvoiceUploadResponse.builder()
                .id(invoice.getId())
                .status(invoice.getStatus())
                .originalFilename(invoice.getOriginalFilename())
                .uploadedAt(invoice.getUploadedAt())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
