package com.zamp.invoice.validation;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OcrConfidenceValidator {

    private final ValidationConfig validationConfig;

    public OcrConfidenceValidator(ValidationConfig validationConfig) {
        this.validationConfig = validationConfig;
    }

    public List<ValidationFailure> validate(Invoice invoice, List<ExtractionEvidence> evidence) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
