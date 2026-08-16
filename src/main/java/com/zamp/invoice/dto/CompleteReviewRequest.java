package com.zamp.invoice.dto;

import com.zamp.invoice.domain.ReviewActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteReviewRequest {

    @NotEmpty
    private List<@Valid FailureResolution> resolutions;

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
}
