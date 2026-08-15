package com.zamp.invoice.dto;

import com.zamp.invoice.domain.FieldName;
import com.zamp.invoice.domain.ReviewActionType;
import com.zamp.invoice.domain.ValidationScope;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ValidationFailureDto(
        UUID id,
        ValidationScope scope,
        UUID lineItemId,
        FieldName fieldName,
        String rule,
        UUID relatedInvoiceId,
        String message,
        boolean resolved,
        OffsetDateTime resolvedAt,
        ReviewActionType action,
        String newValue
) {
}
