package com.zamp.invoice.util;

import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.enums.InvoiceStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Composable {@link Specification} predicates for the invoice search/list API — one per filter,
 * each returning {@code null} (meaning "no restriction") when its filter value is absent, so
 * {@code InvoiceService.listInvoices} can {@code .and()} them together regardless of which
 * filters the caller actually supplied.
 */
public class InvoiceSpecification {

    private InvoiceSpecification() {
    }

    public static Specification<Invoice> hasStatus(InvoiceStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Invoice> excludeStatus(InvoiceStatus excluded) {
        return (root, query, cb) ->
                excluded == null ? null : cb.notEqual(root.get("status"), excluded);
    }

    public static Specification<Invoice> vendorContains(String vendor) {
        return (root, query, cb) ->
                vendor == null || vendor.isBlank() ? null :
                        cb.like(cb.lower(root.get("vendorName")), "%" + vendor.toLowerCase() + "%");
    }

    public static Specification<Invoice> hasCurrency(String currency) {
        return (root, query, cb) ->
                currency == null || currency.isBlank() ? null :
                        cb.equal(cb.upper(root.get("currency")), currency.toUpperCase());
    }

    public static Specification<Invoice> invoiceNumberContains(String invoiceNumber) {
        return (root, query, cb) ->
                invoiceNumber == null || invoiceNumber.isBlank() ? null :
                        cb.like(cb.lower(root.get("invoiceNumber")), "%" + invoiceNumber.toLowerCase() + "%");
    }

    public static Specification<Invoice> dateFrom(LocalDate from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("invoiceDate"), from);
    }

    public static Specification<Invoice> dateTo(LocalDate to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("invoiceDate"), to);
    }

    public static Specification<Invoice> amountMin(BigDecimal min) {
        return (root, query, cb) ->
                min == null ? null : cb.greaterThanOrEqualTo(root.get("totalAmount"), min);
    }

    public static Specification<Invoice> amountMax(BigDecimal max) {
        return (root, query, cb) ->
                max == null ? null : cb.lessThanOrEqualTo(root.get("totalAmount"), max);
    }
}
