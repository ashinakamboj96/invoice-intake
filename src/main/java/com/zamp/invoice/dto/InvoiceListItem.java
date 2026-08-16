package com.zamp.invoice.dto;

import com.zamp.invoice.domain.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

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
