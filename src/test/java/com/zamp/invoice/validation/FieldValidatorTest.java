package com.zamp.invoice.validation;

import com.zamp.invoice.domain.FieldName;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.ValidationFailure;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldValidatorTest {

    private final FieldValidator validator = new FieldValidator();

    private Invoice.InvoiceBuilder validInvoiceBuilder() {
        return Invoice.builder()
                .vendorName("Acme Supplies Inc.")
                .totalAmount(new BigDecimal("297.00"))
                .invoiceDate(LocalDate.of(2026, 3, 14))
                .currency("USD");
    }

    @Test
    void nullVendorProducesMissingRequiredFieldOnVendorName() {
        Invoice invoice = validInvoiceBuilder().vendorName(null).build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).anySatisfy(f -> {
            assertThat(f.getFieldName()).isEqualTo(FieldName.VENDOR_NAME);
            assertThat(f.getRule()).isEqualTo("MISSING_REQUIRED_FIELD");
        });
    }

    @Test
    void nullTotalProducesMissingRequiredFieldOnTotalAmount() {
        Invoice invoice = validInvoiceBuilder().totalAmount(null).build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).anySatisfy(f -> {
            assertThat(f.getFieldName()).isEqualTo(FieldName.TOTAL_AMOUNT);
            assertThat(f.getRule()).isEqualTo("MISSING_REQUIRED_FIELD");
        });
    }

    @Test
    void nullInvoiceDateProducesInvalidDate() {
        Invoice invoice = validInvoiceBuilder().invoiceDate(null).build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).anySatisfy(f -> {
            assertThat(f.getFieldName()).isEqualTo(FieldName.INVOICE_DATE);
            assertThat(f.getRule()).isEqualTo("INVALID_DATE");
        });
    }

    @Test
    void invalidCurrencyProducesInvalidCurrency() {
        Invoice invoice = validInvoiceBuilder().currency("XYZ").build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).anySatisfy(f -> {
            assertThat(f.getFieldName()).isEqualTo(FieldName.CURRENCY);
            assertThat(f.getRule()).isEqualTo("INVALID_CURRENCY");
        });
    }

    @Test
    void validCurrencyProducesNoCurrencyFailure() {
        Invoice invoice = validInvoiceBuilder().currency("INR").build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).noneMatch(f -> f.getFieldName() == FieldName.CURRENCY);
    }

    @Test
    void allFieldsValidReturnsEmptyList() {
        Invoice invoice = validInvoiceBuilder().build();

        List<ValidationFailure> failures = validator.validate(invoice, List.of());

        assertThat(failures).isEmpty();
    }
}
