package com.zamp.invoice.validation;

import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Checks each line item's own arithmetic and completeness, independent of the invoice-level totals {@link InvoiceValidator} reconciles. */
@Component
public class LineItemValidator {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    /**
     * Checks each line item's own arithmetic: {@code quantity × unitPrice} must match
     * {@code amount} within a 0.01 tolerance (skipped if quantity or unit price is missing,
     * since there's nothing to compute against), and every line item must have an amount at all.
     *
     * @param invoice   the parent invoice, referenced on each failure row
     * @param lineItems the invoice's line items
     * @return one failure per line item with a missing or mismatched amount; empty if all check out
     */
    public List<ValidationFailure> validate(Invoice invoice, List<InvoiceLineItem> lineItems) {
        List<ValidationFailure> failures = new ArrayList<>();

        for (InvoiceLineItem lineItem : lineItems) {
            BigDecimal amount = lineItem.getAmount();

            if (amount == null) {
                failures.add(buildFailure(invoice, lineItem, "MISSING_LINE_ITEM_AMOUNT",
                        "Line item " + lineItem.getLineNumber() + " has no extracted amount."));
                continue;
            }

            BigDecimal quantity = lineItem.getQuantity();
            BigDecimal unitPrice = lineItem.getUnitPrice();
            if (quantity != null && unitPrice != null) {
                BigDecimal expected = quantity.multiply(unitPrice);
                if (expected.subtract(amount).abs().compareTo(TOLERANCE) > 0) {
                    String message = "Line " + lineItem.getLineNumber() + ": " + quantity.toPlainString() + " × "
                            + unitPrice.toPlainString() + " = " + expected.toPlainString()
                            + ", but this line shows " + amount.toPlainString() + ". One of these values may be wrong.";
                    failures.add(buildFailure(invoice, lineItem, "LINE_TOTAL_MISMATCH", message));
                }
            }
        }

        return failures;
    }

    private ValidationFailure buildFailure(Invoice invoice, InvoiceLineItem lineItem, String rule, String message) {
        return ValidationFailure.builder()
                .id(UUID.randomUUID())
                .invoice(invoice)
                .scope(ValidationScope.LINE_ITEM)
                .lineItemId(lineItem.getId())
                .rule(rule)
                .message(message)
                .build();
    }
}
