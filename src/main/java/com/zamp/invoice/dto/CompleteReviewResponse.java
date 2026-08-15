package com.zamp.invoice.dto;

import com.zamp.invoice.domain.InvoiceStatus;

import java.util.List;
import java.util.UUID;

public record CompleteReviewResponse(
        UUID invoiceId,
        InvoiceStatus status,
        List<ValidationFailureDto> remainingFailures
) {
}
