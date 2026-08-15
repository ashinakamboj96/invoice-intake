package com.zamp.invoice.dto;

import com.zamp.invoice.domain.ExtractionMethod;
import com.zamp.invoice.domain.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDetailResponse {

    private UUID id;
    private InvoiceStatus status;
    private ExtractionMethod extractionMethod;
    private String originalFilename;
    private OffsetDateTime uploadedAt;
    private String vendorName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String currency;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String failureMessage;
    private List<LineItemDto> lineItems;
    private List<ValidationFailureDto> validationFailures;
}
