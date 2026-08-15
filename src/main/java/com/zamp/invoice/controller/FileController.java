package com.zamp.invoice.controller;

import com.zamp.invoice.service.InvoiceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/invoices/{id}/file")
public class FileController {

    private final InvoiceService invoiceService;

    public FileController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<byte[]> downloadOriginalFile(@PathVariable("id") UUID id) {
        byte[] fileBytes = invoiceService.downloadOriginalFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileBytes);
    }
}
