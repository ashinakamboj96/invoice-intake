package com.zamp.invoice.validation;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.ExtractionMethod;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.domain.ValidationScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class OcrConfidenceValidator {

    private final ValidationConfig validationConfig;

    public OcrConfidenceValidator(ValidationConfig validationConfig) {
        this.validationConfig = validationConfig;
    }

    public List<ValidationFailure> validate(Invoice invoice, List<ExtractionEvidence> evidence, List<InvoiceLineItem> lineItems) {
        if (invoice.getExtractionMethod() != ExtractionMethod.OCR) {
            return List.of();
        }

        BigDecimal threshold = validationConfig.getOcrConfidenceThreshold();
        List<ValidationFailure> failures = new ArrayList<>();

        for (ExtractionEvidence row : evidence) {
            BigDecimal confidence = row.getOcrConfidence();
            if (confidence == null) {
                failures.add(buildFailure(invoice, row, "OCR_SOURCE_NOT_FOUND",
                        "OCR source word could not be located for this field."));
            } else if (confidence.compareTo(threshold) < 0) {
                String message = "OCR confidence was " + toPercent(confidence) + "%, below the "
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
