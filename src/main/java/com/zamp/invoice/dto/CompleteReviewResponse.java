package com.zamp.invoice.dto;

import com.zamp.invoice.domain.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteReviewResponse {

    private InvoiceStatus status;
    private List<ValidationFailureDto> newFailures;
}
