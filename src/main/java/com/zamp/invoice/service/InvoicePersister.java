package com.zamp.invoice.service;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.llm.LlmInvoiceResult;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
public class InvoicePersister {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;

    public InvoicePersister(InvoiceRepository invoiceRepository, InvoiceLineItemRepository invoiceLineItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
    }

    public void persist(UUID invoiceId, LlmInvoiceResult result) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        invoice.setVendorName(result.getVendorName());
        invoice.setInvoiceNumber(result.getInvoiceNumber());
        invoice.setInvoiceDate(parseInvoiceDate(result.getInvoiceDate()));
        invoice.setCurrency(result.getCurrency());
        invoice.setSubtotalAmount(result.getSubtotalAmount());
        invoice.setTaxAmount(result.getTaxAmount());
        invoice.setTotalAmount(result.getTotalAmount());
        invoiceRepository.save(invoice);

        List<LlmInvoiceResult.LlmLineItem> lineItems = result.getLineItems();
        if (lineItems != null) {
            List<InvoiceLineItem> entities = lineItems.stream()
                    .map(item -> InvoiceLineItem.builder()
                            .id(UUID.randomUUID())
                            .invoice(invoice)
                            .lineNumber(item.getLineNumber())
                            .description(item.getDescription())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .amount(item.getAmount())
                            .build())
                    .toList();
            invoiceLineItemRepository.saveAll(entities);
        }
    }

    private LocalDate parseInvoiceDate(String invoiceDate) {
        if (invoiceDate == null) {
            return null;
        }
        try {
            return LocalDate.parse(invoiceDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
