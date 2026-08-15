package com.zamp.invoice.validation;

import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidationEngine {

    private final FieldValidator fieldValidator;
    private final OcrConfidenceValidator ocrConfidenceValidator;
    private final LineItemValidator lineItemValidator;
    private final InvoiceValidator invoiceValidator;
    private final DuplicateDetector duplicateDetector;

    public ValidationEngine(FieldValidator fieldValidator,
                             OcrConfidenceValidator ocrConfidenceValidator,
                             LineItemValidator lineItemValidator,
                             InvoiceValidator invoiceValidator,
                             DuplicateDetector duplicateDetector) {
        this.fieldValidator = fieldValidator;
        this.ocrConfidenceValidator = ocrConfidenceValidator;
        this.lineItemValidator = lineItemValidator;
        this.invoiceValidator = invoiceValidator;
        this.duplicateDetector = duplicateDetector;
    }

    public List<ValidationFailure> validate(Invoice invoice, List<ExtractionEvidence> evidence) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
