package com.zamp.invoice.llm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LlmInvoiceResult(
        String vendorName,
        String invoiceNumber,
        LocalDate invoiceDate,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        List<LineItem> lineItems
) {

    public record LineItem(
            Integer lineNumber,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {
    }
}
