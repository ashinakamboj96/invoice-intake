package com.zamp.invoice.validation;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FieldValidator {

    public List<ValidationFailure> validate(Invoice invoice) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
