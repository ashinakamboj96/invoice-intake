package com.zamp.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LineItemDto(
        UUID id,
        Integer lineNumber,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {
}
