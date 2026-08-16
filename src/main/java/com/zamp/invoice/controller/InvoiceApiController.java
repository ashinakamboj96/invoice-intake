package com.zamp.invoice.controller;

import com.zamp.invoice.enums.InvoiceStatus;
import com.zamp.invoice.model.dto.InvoiceDetailResponse;
import com.zamp.invoice.model.dto.InvoiceListResponse;
import com.zamp.invoice.service.InvoiceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * JSON REST API for invoices, kept under {@code /api} so its {@code GET /{id}} route doesn't collide
 * with {@link InvoiceViewController}'s HTML page at the same {@code /invoices/{id}} path.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceApiController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InvoiceService invoiceService;

    public InvoiceApiController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailResponse> getInvoice(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @GetMapping
    public ResponseEntity<InvoiceListResponse> listInvoices(
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "vendor", required = false) String vendor,
            @RequestParam(value = "invoiceNumber", required = false) String invoiceNumber,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DATE) LocalDate dateTo,
            @RequestParam(value = "amountMin", required = false) BigDecimal amountMin,
            @RequestParam(value = "amountMax", required = false) BigDecimal amountMax,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        return ResponseEntity.ok(invoiceService.listInvoices(
                status, vendor, invoiceNumber, currency, dateFrom, dateTo, amountMin, amountMax, page, cappedSize));
    }
}
