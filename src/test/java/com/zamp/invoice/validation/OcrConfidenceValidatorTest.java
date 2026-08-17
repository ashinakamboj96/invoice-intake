package com.zamp.invoice.validation;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.model.entity.ExtractionEvidence;
import com.zamp.invoice.enums.ExtractionMethod;
import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
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
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR)
                .totalAmount(new BigDecimal("6490")).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.TOTAL_AMOUNT)
                .ocrConfidence(new BigDecimal("0.61"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("LOW_OCR_CONFIDENCE");
        assertThat(failures.get(0).getFieldName()).isEqualTo(FieldName.TOTAL_AMOUNT);
        assertThat(failures.get(0).getScope()).isEqualTo(ValidationScope.INVOICE_FIELD);
        assertThat(failures.get(0).getMessage()).contains("Total amount was read as \"6490\" with 61% confidence");
    }

    @Test
    void nullConfidenceProducesSourceWordNotLocatedFailure() {
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR)
                .vendorName("Acme Supplies Inc.").build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.VENDOR_NAME)
                .ocrConfidence(null)
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("OCR_SOURCE_NOT_FOUND");
        assertThat(failures.get(0).getMessage())
                .contains("Vendor name was read as \"Acme Supplies Inc.\"")
                .contains("could not be located");
    }

    @Test
    void missingExtractedValueFallsBackToUnknownInMessage() {
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.VENDOR_NAME)
                .ocrConfidence(new BigDecimal("0.40"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getMessage()).contains("read as \"unknown\"");
    }

    @Test
    void nullConfidenceOnDescriptionProducesNoFailure() {
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.DESCRIPTION)
                .lineItemId(UUID.randomUUID())
                .ocrConfidence(null)
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).isEmpty();
    }

    @Test
    void nullConfidenceOnTotalAmountStillProducesFailure() {
        Invoice invoice = Invoice.builder().extractionMethod(ExtractionMethod.OCR)
                .totalAmount(new BigDecimal("6490")).build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.TOTAL_AMOUNT)
                .ocrConfidence(null)
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of());

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("OCR_SOURCE_NOT_FOUND");
        assertThat(failures.get(0).getFieldName()).isEqualTo(FieldName.TOTAL_AMOUNT);
        assertThat(failures.get(0).getMessage()).contains("Total amount was read as \"6490\"");
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
        InvoiceLineItem lineItem = InvoiceLineItem.builder()
                .id(lineItemId)
                .amount(new BigDecimal("5800"))
                .build();
        ExtractionEvidence evidenceRow = ExtractionEvidence.builder()
                .fieldName(FieldName.AMOUNT)
                .lineItemId(lineItemId)
                .ocrConfidence(new BigDecimal("0.50"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(evidenceRow), List.of(lineItem));

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getScope()).isEqualTo(ValidationScope.LINE_ITEM);
        assertThat(failures.get(0).getLineItemId()).isEqualTo(lineItemId);
        assertThat(failures.get(0).getMessage()).contains("Line amount was read as \"5800\"");
    }
}
