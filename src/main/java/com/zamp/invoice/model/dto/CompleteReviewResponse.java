package com.zamp.invoice.model.dto;

import com.zamp.invoice.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response for {@code POST /invoices/{id}/complete-review}: the invoice's status after revalidation, and any newly-surfaced failures. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteReviewResponse {

    private InvoiceStatus status;
    private List<ValidationFailureDto> newFailures;
}
