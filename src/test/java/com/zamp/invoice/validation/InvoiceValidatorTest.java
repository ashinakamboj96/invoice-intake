package com.zamp.invoice.validation;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.domain.ValidationFailure;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceValidatorTest {

    private final InvoiceValidator validator = new InvoiceValidator();

    private InvoiceLineItem lineItemWithAmount(BigDecimal amount) {
        return InvoiceLineItem.builder()
                .id(UUID.randomUUID())
                .lineNumber(1)
                .amount(amount)
                .build();
    }

    @Test
    void matchingLineItemSumAndSubtotalProducesNoFailure() {
        Invoice invoice = Invoice.builder()
                .subtotalAmount(new BigDecimal("25.00"))
                .build();
        List<InvoiceLineItem> lineItems = List.of(lineItemWithAmount(new BigDecimal("25.00")));

        List<ValidationFailure> failures = validator.validate(invoice, lineItems);

        assertThat(failures).isEmpty();
    }

    @Test
    void mismatchedLineItemSumAndSubtotalProducesSubtotalMismatch() {
        Invoice invoice = Invoice.builder()
                .subtotalAmount(new BigDecimal("25.00"))
                .build();
        List<InvoiceLineItem> lineItems = List.of(lineItemWithAmount(new BigDecimal("20.00")));

        List<ValidationFailure> failures = validator.validate(invoice, lineItems);

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("SUBTOTAL_MISMATCH");
    }

    @Test
    void matchingSubtotalPlusTaxAndTotalProducesNoFailure() {
        Invoice invoice = Invoice.builder()
                .subtotalAmount(new BigDecimal("25.00"))
                .taxAmount(new BigDecimal("2.00"))
                .totalAmount(new BigDecimal("27.00"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).isEmpty();
    }

    @Test
    void mismatchedSubtotalPlusTaxAndTotalProducesTotalReconciliation() {
        Invoice invoice = Invoice.builder()
                .subtotalAmount(new BigDecimal("25.00"))
                .taxAmount(new BigDecimal("2.00"))
                .totalAmount(new BigDecimal("30.00"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("TOTAL_RECONCILIATION");
    }

    @Test
    void nullSubtotalWithLineItemsUsesLineItemSumForReconciliation() {
        Invoice invoice = Invoice.builder()
                .subtotalAmount(null)
                .taxAmount(null)
                .totalAmount(new BigDecimal("25.00"))
                .build();
        List<InvoiceLineItem> lineItems = List.of(lineItemWithAmount(new BigDecimal("25.00")));

        List<ValidationFailure> failures = validator.validate(invoice, lineItems);

        assertThat(failures).noneMatch(f -> f.getRule().equals("TOTAL_RECONCILIATION"));
    }

    @Test
    void allNullsProducesNoFailures() {
        Invoice invoice = Invoice.builder()
                .subtotalAmount(null)
                .taxAmount(null)
                .totalAmount(null)
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).isEmpty();
    }
}
