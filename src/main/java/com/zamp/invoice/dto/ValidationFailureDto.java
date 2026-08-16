package com.zamp.invoice.dto;

import com.zamp.invoice.domain.FieldName;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.domain.ValidationScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String message;
    private UUID relatedInvoiceId;
    private boolean resolved;

    public static ValidationFailureDto from(ValidationFailure failure) {
        return ValidationFailureDto.builder()
                .id(failure.getId())
                .scope(failure.getScope())
                .lineItemId(failure.getLineItemId())
                .fieldName(failure.getFieldName())
                .rule(failure.getRule())
                .message(failure.getMessage())
                .relatedInvoiceId(failure.getRelatedInvoice() != null ? failure.getRelatedInvoice().getId() : null)
                .resolved(failure.isResolved())
                .build();
    }
}
