package com.zamp.invoice.validation;

import com.zamp.invoice.config.ValidationProperties;
import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OcrConfidenceValidator {

    private final ValidationProperties validationProperties;

    public OcrConfidenceValidator(ValidationProperties validationProperties) {
        this.validationProperties = validationProperties;
    }

    public List<ValidationFailure> validate(Invoice invoice, List<ExtractionEvidence> evidence) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
