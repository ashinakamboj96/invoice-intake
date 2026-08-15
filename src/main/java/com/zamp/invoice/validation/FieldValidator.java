package com.zamp.invoice.validation;

import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.FieldName;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.domain.ValidationScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class FieldValidator {

    private static final Set<String> VALID_CURRENCY_CODES = Set.of(
            "USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "SGD", "AED"
    );

    public List<ValidationFailure> validate(Invoice invoice, List<ExtractionEvidence> evidence) {
        List<ValidationFailure> failures = new ArrayList<>();

        if (invoice.getVendorName() == null || invoice.getVendorName().isBlank()) {
            failures.add(buildFailure(invoice, FieldName.VENDOR_NAME, "MISSING_REQUIRED_FIELD",
                    "Vendor name could not be extracted from the document."));
        }

        if (invoice.getTotalAmount() == null) {
            failures.add(buildFailure(invoice, FieldName.TOTAL_AMOUNT, "MISSING_REQUIRED_FIELD",
                    "Total amount could not be extracted from the document."));
        }

        if (invoice.getInvoiceDate() == null) {
            failures.add(buildFailure(invoice, FieldName.INVOICE_DATE, "INVALID_DATE",
                    "Invoice date could not be extracted or is not a valid date."));
        }

        String currency = invoice.getCurrency();
        if (currency != null && !VALID_CURRENCY_CODES.contains(currency.toUpperCase())) {
            failures.add(buildFailure(invoice, FieldName.CURRENCY, "INVALID_CURRENCY",
                    "Extracted currency '" + currency + "' is not a recognised currency code."));
        }

        return failures;
    }

    private ValidationFailure buildFailure(Invoice invoice, FieldName fieldName, String rule, String message) {
        return ValidationFailure.builder()
                .id(UUID.randomUUID())
                .invoice(invoice)
                .scope(ValidationScope.INVOICE_FIELD)
                .fieldName(fieldName)
                .rule(rule)
                .message(message)
                .build();
    }
}
