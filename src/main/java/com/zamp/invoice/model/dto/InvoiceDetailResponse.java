package com.zamp.invoice.model.dto;

import com.zamp.invoice.enums.ExtractionMethod;
import com.zamp.invoice.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Full detail payload for {@code GET /api/invoices/{id}} and the HTML detail page: every field, line item, and failure a reviewer needs for one invoice. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDetailResponse {

    private UUID id;
    private String vendorName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String currency;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private ExtractionMethod extractionMethod;
    private OffsetDateTime uploadedAt;
    private String failureMessage;

    private List<LineItemDto> lineItems;

    private List<ValidationFailureDto> unresolvedFailures;
    private List<ValidationFailureDto> resolvedFailures;

    /** Field name to OCR confidence, invoice-level fields only; empty for PDF_TEXT invoices. */
    private Map<String, Double> evidenceSummary;

    /** The invoice this one was confirmed a duplicate of; null unless {@code status} is {@code REJECTED}. */
    private UUID relatedInvoiceId;
}
