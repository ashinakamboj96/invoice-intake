package com.zamp.invoice.validation;

import com.zamp.invoice.model.entity.ExtractionEvidence;
import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Presence/format checks on invoice-level fields, independent of OCR confidence or arithmetic. */
@Component
public class FieldValidator {

    private static final Set<String> VALID_CURRENCY_CODES = Set.of(
            "USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "SGD", "AED"
    );

    /**
     * Checks required invoice-level fields are present and, where applicable, well-formed:
     * vendor name and total amount must be non-null, invoice date must be present, and currency
     * (when present) must be a recognised ISO 4217 code.
     *
     * @param invoice  the invoice to check
     * @param evidence unused; kept so this method matches the shared validator call signature
     * @return one failure per rule that fired; empty if all fields are valid
     */
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
