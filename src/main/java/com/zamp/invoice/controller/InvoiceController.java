package com.zamp.invoice.controller;

import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.dto.InvoiceDetailResponse;
import com.zamp.invoice.dto.InvoiceListResponse;
import com.zamp.invoice.dto.InvoiceUploadResponse;
import com.zamp.invoice.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<InvoiceUploadResponse> uploadInvoice(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(invoiceService.uploadInvoice(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailResponse> getInvoice(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @GetMapping
    public ResponseEntity<InvoiceListResponse> listInvoices(
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(invoiceService.listInvoices(status, page, pageSize));
    }
}
