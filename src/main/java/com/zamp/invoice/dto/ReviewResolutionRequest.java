package com.zamp.invoice.dto;

import com.zamp.invoice.domain.ReviewActionType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewResolutionRequest(
        @NotNull UUID validationFailureId,
        @NotNull ReviewActionType action,
        String newValue
) {
}
