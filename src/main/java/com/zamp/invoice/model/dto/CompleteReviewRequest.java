package com.zamp.invoice.model.dto;

import com.zamp.invoice.enums.ReviewActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Request body for {@code POST /invoices/{id}/complete-review}: one resolution per currently-unresolved failure. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteReviewRequest {

    @NotEmpty
    private List<@Valid FailureResolution> resolutions;

    /** Direct edits to the extracted fields panel, independent of any failure-card resolution above. Optional — null/absent is treated as no edits. */
    private List<@Valid FieldCorrection> fieldCorrections;

    /** How a reviewer resolved one {@code ValidationFailure}; {@code newValue} is required only when {@code action} is {@code CORRECTED}. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureResolution {

        @NotNull
        private UUID failureId;

        @NotNull
        private ReviewActionType action;

        private String newValue;
    }

    /** A raw edit to one field in the extracted fields panel; {@code lineItemId} is null for invoice-level fields. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldCorrection {

        @NotNull
        private String fieldName;

        private UUID lineItemId;

        @NotNull
        private String newValue;
    }
}
