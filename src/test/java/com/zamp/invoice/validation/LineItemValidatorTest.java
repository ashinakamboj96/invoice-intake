package com.zamp.invoice.validation;

import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.model.entity.ValidationFailure;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LineItemValidatorTest {

    private final LineItemValidator validator = new LineItemValidator();
    private final Invoice invoice = Invoice.builder().build();

    @Test
    void matchingQuantityTimesUnitPriceProducesNoFailure() {
        InvoiceLineItem lineItem = InvoiceLineItem.builder()
                .id(UUID.randomUUID())
                .lineNumber(1)
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("12.50"))
                .amount(new BigDecimal("25.00"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(lineItem));

        assertThat(failures).isEmpty();
    }

    @Test
    void mismatchedAmountProducesLineTotalMismatch() {
        InvoiceLineItem lineItem = InvoiceLineItem.builder()
                .id(UUID.randomUUID())
                .lineNumber(1)
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("12.50"))
                .amount(new BigDecimal("30.00"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(lineItem));

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("LINE_TOTAL_MISMATCH");
    }

    @Test
    void nullAmountProducesMissingLineItemAmount() {
        InvoiceLineItem lineItem = InvoiceLineItem.builder()
                .id(UUID.randomUUID())
                .lineNumber(1)
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("12.50"))
                .amount(null)
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(lineItem));

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("MISSING_LINE_ITEM_AMOUNT");
    }

    @Test
    void nullQuantityOrUnitPriceSkipsMismatchCheck() {
        InvoiceLineItem missingQuantity = InvoiceLineItem.builder()
                .id(UUID.randomUUID())
                .lineNumber(1)
                .quantity(null)
                .unitPrice(new BigDecimal("12.50"))
                .amount(new BigDecimal("30.00"))
                .build();
        InvoiceLineItem missingUnitPrice = InvoiceLineItem.builder()
                .id(UUID.randomUUID())
                .lineNumber(2)
                .quantity(new BigDecimal("2"))
                .unitPrice(null)
                .amount(new BigDecimal("30.00"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(missingQuantity, missingUnitPrice));

        assertThat(failures).isEmpty();
    }

    @Test
    void withinToleranceProducesNoFailure() {
        InvoiceLineItem lineItem = InvoiceLineItem.builder()
                .id(UUID.randomUUID())
                .lineNumber(1)
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("12.50"))
                .amount(new BigDecimal("25.01"))
                .build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of(lineItem));

        assertThat(failures).isEmpty();
    }
}
