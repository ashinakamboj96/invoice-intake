package com.zamp.invoice.dto;

import com.zamp.invoice.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceListItem(
        UUID id,
        String vendorName,
        String invoiceNumber,
        LocalDate invoiceDate,
        BigDecimal totalAmount,
        InvoiceStatus status,
        OffsetDateTime uploadedAt
) {
}
