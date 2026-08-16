package com.zamp.invoice.model.dto;

import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** API projection of {@code ValidationFailure} — omits reviewer-internal fields like {@code action}/{@code newValue} that the client doesn't need to read back. */
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

    /** Maps a {@code ValidationFailure} entity to its API projection. */
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
