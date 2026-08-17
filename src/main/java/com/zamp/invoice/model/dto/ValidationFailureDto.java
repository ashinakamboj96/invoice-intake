package com.zamp.invoice.model.dto;

import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.enums.ReviewActionType;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** API projection of {@code ValidationFailure}. {@code action}/{@code newValue} are included (read-only) so the review-history UI can show what a reviewer actually did, not just the original failure message. */
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
    private ReviewActionType action;
    private String newValue;
    /** "Line 3 — Widget A" for {@code LINE_ITEM}-scope failures; null otherwise. Populated by {@code InvoiceService}, which has the line items in hand — not derivable from the entity alone. */
    private String lineDescription;

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
                .action(failure.getAction())
                .newValue(failure.getNewValue())
                .build();
    }
}
