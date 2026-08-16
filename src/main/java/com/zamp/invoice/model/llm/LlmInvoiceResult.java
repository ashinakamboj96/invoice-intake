package com.zamp.invoice.model.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * The LLM's raw structuring output — a direct deserialization target for its JSON response, before
 * {@code InvoicePersister} maps it onto the {@code Invoice} entity. Fields are left as {@code
 * String} (e.g. {@code invoiceDate}) rather than parsed types where the LLM's output still needs
 * validation/parsing downstream; any field the LLM couldn't find in the source text is null,
 * never guessed or defaulted.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmInvoiceResult {

    private String vendorName;
    private String invoiceNumber;
    private String invoiceDate;
    private String currency;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private List<LlmLineItem> lineItems;

    /** One line item as the LLM read it, before persistence. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LlmLineItem {

        private Integer lineNumber;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
    }
}
