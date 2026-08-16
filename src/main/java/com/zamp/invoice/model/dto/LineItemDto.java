package com.zamp.invoice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** API projection of {@code InvoiceLineItem} — same fields, decoupled from the JPA entity. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItemDto {

    private UUID id;
    private Integer lineNumber;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
