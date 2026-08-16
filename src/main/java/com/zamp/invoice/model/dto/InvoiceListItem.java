package com.zamp.invoice.model.dto;

import com.zamp.invoice.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** One row in the invoice list/search results — a summary projection, not the full invoice. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceListItem {

    private UUID id;
    private String vendorName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    private String currency;
    private InvoiceStatus status;
    private OffsetDateTime uploadedAt;
    private int unresolvedFailureCount;
}
