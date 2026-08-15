package com.zamp.invoice.dto;

import com.zamp.invoice.domain.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteReviewResponse {

    private UUID invoiceId;
    private InvoiceStatus status;
    private List<ValidationFailureDto> remainingFailures;
}
