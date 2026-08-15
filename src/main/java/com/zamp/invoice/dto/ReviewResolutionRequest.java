package com.zamp.invoice.dto;

import com.zamp.invoice.domain.ReviewActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResolutionRequest {

    @NotNull
    private UUID validationFailureId;

    @NotNull
    private ReviewActionType action;

    private String newValue;
}
