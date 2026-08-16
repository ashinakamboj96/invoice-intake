package com.zamp.invoice.controller;

import com.zamp.invoice.enums.InvoiceStatus;
import com.zamp.invoice.model.dto.InvoiceListResponse;
import com.zamp.invoice.service.InvoiceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/** Server-rendered Thymeleaf pages; the JSON API for the same resources lives under {@code /api/invoices}. */
@Controller
public class InvoiceViewController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final InvoiceService invoiceService;

    public InvoiceViewController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/")
    public String listInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        InvoiceListResponse result = invoiceService.listInvoices(
                status, vendor, dateFrom, dateTo, amountMin, amountMax, page, size);

        model.addAttribute("invoices", result.getInvoices());
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("currentPage", result.getCurrentPage());
        model.addAttribute("pageSize", size == 0 ? DEFAULT_PAGE_SIZE : size);
        model.addAttribute("statuses", InvoiceStatus.values());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentVendor", vendor);
        model.addAttribute("currentDateFrom", dateFrom);
        model.addAttribute("currentDateTo", dateTo);
        model.addAttribute("currentAmountMin", amountMin);
        model.addAttribute("currentAmountMax", amountMax);
        return "invoices/list";
    }

    @GetMapping("/invoices/{id}")
    public String invoiceDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("invoice", invoiceService.getInvoice(id));
        return "invoices/detail";
    }
}
