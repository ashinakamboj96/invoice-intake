package com.zamp.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteReviewRequest(
        @NotEmpty List<@Valid ReviewResolutionRequest> resolutions
) {
}
