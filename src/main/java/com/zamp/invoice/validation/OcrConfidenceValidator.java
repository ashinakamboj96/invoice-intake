package com.zamp.invoice.validation;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.model.entity.ExtractionEvidence;
import com.zamp.invoice.enums.ExtractionMethod;
import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * @param lineItems the invoice's line items, used only to name the line number in a
     *                  line-item-scoped failure's message (e.g. "Line 3: ...")
     * @return one failure per low/missing-confidence evidence row; empty for non-OCR invoices
     *         or when every field met the threshold
     */
    public List<ValidationFailure> validate(Invoice invoice, List<ExtractionEvidence> evidence, List<InvoiceLineItem> lineItems) {
        if (invoice.getExtractionMethod() != ExtractionMethod.OCR) {
            return List.of();
        }

        Map<UUID, Integer> lineNumbersById = lineItems.stream()
                .collect(Collectors.toMap(InvoiceLineItem::getId, InvoiceLineItem::getLineNumber));

        BigDecimal threshold = validationConfig.getOcrConfidenceThreshold();
        List<ValidationFailure> failures = new ArrayList<>();

        for (ExtractionEvidence row : evidence) {
            Integer lineNumber = row.getLineItemId() == null ? null : lineNumbersById.get(row.getLineItemId());
            String linePrefix = lineNumber == null ? "" : "Line " + lineNumber + ": ";
            BigDecimal confidence = row.getOcrConfidence();
            if (confidence == null) {
                failures.add(buildFailure(invoice, row, "OCR_SOURCE_NOT_FOUND",
                        linePrefix + "OCR source word could not be located for this field."));
            } else if (confidence.compareTo(threshold) < 0) {
                String message = linePrefix + "OCR confidence was " + toPercent(confidence) + "%, below the "
                        + toPercent(threshold) + "% review threshold.";
                failures.add(buildFailure(invoice, row, "LOW_OCR_CONFIDENCE", message));
            }
        }

        return failures;
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
