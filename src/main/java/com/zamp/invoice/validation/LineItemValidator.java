package com.zamp.invoice.validation;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.domain.ValidationScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class LineItemValidator {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

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
                    String message = quantity.toPlainString() + " × " + unitPrice.toPlainString() + " = "
                            + expected.toPlainString() + ", but extracted amount is " + amount.toPlainString() + ".";
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
