package com.zamp.invoice.validation;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.ExtractionMethod;
import com.zamp.invoice.domain.FieldName;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.domain.ValidationScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OcrConfidenceValidatorTest {

    private final ValidationConfig validationConfig = new ValidationConfig();
    private final OcrConfidenceValidator validator = new OcrConfidenceValidator(validationConfig);

    @Test
    void pdfTextInvoiceReturnsEmptyListWithoutCheckingEvidence() {
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.PDF_TEXT).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.TOTAL_AMOUNT)
                .ocrConfidence(new BigDecimal("0.10"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).isEmpty();
    }

    @Test
    void lowConfidenceOnTotalAmountProducesLowOcrConfidenceFailure() {
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.TOTAL_AMOUNT)
                .ocrConfidence(new BigDecimal("0.61"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("LOW_OCR_CONFIDENCE");
        assertThat(failures.get(0).getFieldName()).isEqualTo(FieldName.TOTAL_AMOUNT);
        assertThat(failures.get(0).getScope()).isEqualTo(ValidationScope.INVOICE_FIELD);
    }

    @Test
    void nullConfidenceProducesSourceWordNotLocatedFailure() {
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.VENDOR_NAME)
                .ocrConfidence(null)
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getMessage()).contains("source word could not be located");
    }

    @Test
    void highConfidenceProducesNoFailure() {
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.TOTAL_AMOUNT)
                .ocrConfidence(new BigDecimal("0.95"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).isEmpty();
    }

    @Test
    void lineItemEvidenceWithLowConfidenceProducesLineItemScopeFailure() {
        UUID lineItemId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.AMOUNT)
                .lineItemId(lineItemId)
                .ocrConfidence(new BigDecimal("0.50"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getScope()).isEqualTo(ValidationScope.LINE_ITEM);
        assertThat(failures.get(0).getLineItemId()).isEqualTo(lineItemId);
    }
}
