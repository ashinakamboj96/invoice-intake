package com.zamp.invoice.validation;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.model.entity.ExtractionEvidence;
import com.zamp.invoice.enums.ExtractionMethod;
import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Flags low-confidence and unmatched OCR evidence — the validator that turns {@code EvidenceMapper}'s output into reviewer-facing warnings. */
@Component
public class OcrConfidenceValidator {

    private final ValidationConfig validationConfig;

    public OcrConfidenceValidator(ValidationConfig validationConfig) {
        this.validationConfig = validationConfig;
    }

    /**
     * Flags every OCR-sourced field whose confidence is below
     * {@link ValidationConfig#getOcrConfidenceThreshold()}, or whose source word couldn't be
     * located at all. No-op for {@code PDF_TEXT} invoices, since there's no OCR confidence to
     * check.
     *
     * @param invoice   the invoice being checked; only acted on when its extraction method is OCR
     * @param evidence  the OCR evidence rows collected for this invoice
     * @param lineItems the invoice's line items, used to include the extracted value for
     *                  line-item-scoped fields in the message (e.g. "Quantity was read as \"30\"")
     * @return one failure per low/missing-confidence evidence row; empty for non-OCR invoices
     *         or when every field met the threshold
     */
    public List<ValidationFailure> validate(Invoice invoice, List<ExtractionEvidence> evidence, List<InvoiceLineItem> lineItems) {
        if (invoice.getExtractionMethod() != ExtractionMethod.OCR) {
            return List.of();
        }

        BigDecimal threshold = validationConfig.getOcrConfidenceThreshold();
        List<ValidationFailure> failures = new ArrayList<>();

        for (ExtractionEvidence row : evidence) {
            BigDecimal confidence = row.getOcrConfidence();

            // DESCRIPTION is multi-word free text and frequently has no single matching OCR word
            // even when the extraction is fine — suppressing this noise; the description is still
            // visible and editable in the line items table, so nothing is hidden from the reviewer.
            if (confidence == null && row.getFieldName() == FieldName.DESCRIPTION) {
                continue;
            }

            String label = toHumanLabel(row.getFieldName());
            String extractedValue = getExtractedValue(row.getFieldName(), invoice, lineItems, row.getLineItemId());
            String valueDisplay = extractedValue != null ? extractedValue : "unknown";
            if (confidence == null) {
                String message = String.format(
                        "%s was read as \"%s\" but could not be located in the scanned document. Please verify this value is correct.",
                        label, valueDisplay);
                failures.add(buildFailure(invoice, row, "OCR_SOURCE_NOT_FOUND", message));
            } else if (confidence.compareTo(threshold) < 0) {
                String message = String.format(
                        "%s was read as \"%s\" with %s%% confidence (our threshold is %s%%). "
                                + "Please check this value against the original document and correct it if needed.",
                        label, valueDisplay, toPercent(confidence), toPercent(threshold));
                failures.add(buildFailure(invoice, row, "LOW_OCR_CONFIDENCE", message));
            }
        }

        return failures;
    }

    private String getExtractedValue(FieldName fieldName, Invoice invoice, List<InvoiceLineItem> lineItems, UUID lineItemId) {
        if (lineItemId != null) {
            return lineItems.stream()
                    .filter(li -> li.getId().equals(lineItemId))
                    .findFirst()
                    .map(li -> switch (fieldName) {
                        case DESCRIPTION -> li.getDescription();
                        case QUANTITY -> li.getQuantity() != null ? li.getQuantity().toPlainString() : null;
                        case UNIT_PRICE -> li.getUnitPrice() != null ? li.getUnitPrice().toPlainString() : null;
                        case AMOUNT -> li.getAmount() != null ? li.getAmount().toPlainString() : null;
                        default -> null;
                    })
                    .orElse(null);
        }
        return switch (fieldName) {
            case VENDOR_NAME -> invoice.getVendorName();
            case INVOICE_NUMBER -> invoice.getInvoiceNumber();
            case INVOICE_DATE -> invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : null;
            case CURRENCY -> invoice.getCurrency();
            case SUBTOTAL_AMOUNT -> invoice.getSubtotalAmount() != null ? invoice.getSubtotalAmount().toPlainString() : null;
            case TAX_AMOUNT -> invoice.getTaxAmount() != null ? invoice.getTaxAmount().toPlainString() : null;
            case TOTAL_AMOUNT -> invoice.getTotalAmount() != null ? invoice.getTotalAmount().toPlainString() : null;
            default -> null;
        };
    }

    private String toHumanLabel(FieldName fieldName) {
        return switch (fieldName) {
            case VENDOR_NAME -> "Vendor name";
            case INVOICE_NUMBER -> "Invoice number";
            case INVOICE_DATE -> "Invoice date";
            case CURRENCY -> "Currency";
            case SUBTOTAL_AMOUNT -> "Subtotal";
            case TAX_AMOUNT -> "Tax amount";
            case TOTAL_AMOUNT -> "Total amount";
            case DESCRIPTION -> "Description";
            case QUANTITY -> "Quantity";
            case UNIT_PRICE -> "Unit price";
            case AMOUNT -> "Line amount";
        };
    }

    private String toPercent(BigDecimal fraction) {
        return fraction.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private ValidationFailure buildFailure(Invoice invoice, ExtractionEvidence row, String rule, String message) {
        ValidationFailure.ValidationFailureBuilder builder = ValidationFailure.builder()
                .id(UUID.randomUUID())
                .invoice(invoice)
                .fieldName(row.getFieldName())
                .rule(rule)
                .message(message);

        if (row.getLineItemId() == null) {
            builder.scope(ValidationScope.INVOICE_FIELD);
        } else {
            builder.scope(ValidationScope.LINE_ITEM)
                    .lineItemId(row.getLineItemId());
        }

        return builder.build();
    }
}
