package com.zamp.invoice.validation;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.domain.ValidationScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class InvoiceValidator {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    /**
     * Cross-checks invoice-level totals against the line items: the sum of line item amounts
     * must match the extracted subtotal (when both exist), and subtotal + tax must match the
     * extracted total (falling back to the line item sum when subtotal is missing) — both within
     * a 0.01 tolerance.
     *
     * @param invoice   the invoice whose totals are being reconciled
     * @param lineItems the invoice's line items, summed for comparison
     * @return up to two failures (subtotal mismatch, total reconciliation); empty if there's
     *         nothing to check or everything reconciles
     */
    public List<ValidationFailure> validate(Invoice invoice, List<InvoiceLineItem> lineItems) {
        List<ValidationFailure> failures = new ArrayList<>();

        BigDecimal lineItemSum = sumLineItemAmounts(lineItems);

        if (invoice.getSubtotalAmount() != null && !lineItems.isEmpty()) {
            BigDecimal subtotal = invoice.getSubtotalAmount();
            if (lineItemSum.subtract(subtotal).abs().compareTo(TOLERANCE) > 0) {
                String message = "Line items sum to " + lineItemSum.toPlainString()
                        + ", but extracted subtotal is " + subtotal.toPlainString() + ".";
                failures.add(buildFailure(invoice, "SUBTOTAL_MISMATCH", message));
            }
        }

        if (invoice.getTotalAmount() != null) {
            BigDecimal subtotalForReconciliation = invoice.getSubtotalAmount() != null
                    ? invoice.getSubtotalAmount()
                    : lineItemSum;
            BigDecimal tax = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO;
            BigDecimal expected = subtotalForReconciliation.add(tax);
            BigDecimal total = invoice.getTotalAmount();

            if (expected.subtract(total).abs().compareTo(TOLERANCE) > 0) {
                String message = "Expected total is " + expected.toPlainString() + " (" + subtotalForReconciliation.toPlainString()
                        + " + " + tax.toPlainString() + "), but extracted total is " + total.toPlainString() + ".";
                failures.add(buildFailure(invoice, "TOTAL_RECONCILIATION", message));
            }
        }

        return failures;
    }

    private BigDecimal sumLineItemAmounts(List<InvoiceLineItem> lineItems) {
        return lineItems.stream()
                .map(InvoiceLineItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ValidationFailure buildFailure(Invoice invoice, String rule, String message) {
        return ValidationFailure.builder()
                .id(UUID.randomUUID())
                .invoice(invoice)
                .scope(ValidationScope.INVOICE)
                .rule(rule)
                .message(message)
                .build();
    }
}
