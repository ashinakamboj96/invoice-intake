package com.zamp.invoice.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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
