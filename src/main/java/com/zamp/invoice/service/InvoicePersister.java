package com.zamp.invoice.service;

import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.model.llm.LlmInvoiceResult;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/** Writes the LLM's structured output onto the {@code Invoice} entity and its line items — the one place {@code LlmInvoiceResult} gets turned into persisted domain state. */
@Service
public class InvoicePersister {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;

    public InvoicePersister(InvoiceRepository invoiceRepository, InvoiceLineItemRepository invoiceLineItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
    }

    /**
     * @param invoiceId the invoice to update; must already exist
     * @param result    the LLM's reading of the invoice; an unparseable {@code invoiceDate} is
     *                  stored as null rather than failing the whole persist
     */
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
