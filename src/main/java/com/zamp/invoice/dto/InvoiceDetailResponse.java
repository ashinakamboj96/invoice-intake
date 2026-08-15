package com.zamp.invoice.dto;

import com.zamp.invoice.domain.ExtractionMethod;
import com.zamp.invoice.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceDetailResponse(
        UUID id,
        InvoiceStatus status,
        ExtractionMethod extractionMethod,
        String originalFilename,
        OffsetDateTime uploadedAt,
        String vendorName,
        String invoiceNumber,
        LocalDate invoiceDate,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String failureMessage,
        List<LineItemDto> lineItems,
        List<ValidationFailureDto> validationFailures
) {
}
