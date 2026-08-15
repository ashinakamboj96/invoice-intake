package com.zamp.invoice.dto;

import com.zamp.invoice.domain.FieldName;
import com.zamp.invoice.domain.ReviewActionType;
import com.zamp.invoice.domain.ValidationScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationFailureDto {

    private UUID id;
    private ValidationScope scope;
    private UUID lineItemId;
    private FieldName fieldName;
    private String rule;
    private UUID relatedInvoiceId;
    private String message;
    private boolean resolved;
    private OffsetDateTime resolvedAt;
    private ReviewActionType action;
    private String newValue;
}
