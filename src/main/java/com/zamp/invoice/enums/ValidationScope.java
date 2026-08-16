package com.zamp.invoice.enums;

/** What a {@code ValidationFailure} is about: one invoice field, one line item, or the invoice as a whole. */
public enum ValidationScope {
    /** Concerns a single invoice-level field (e.g. {@code TOTAL_AMOUNT}); {@code fieldName} is set. */
    INVOICE_FIELD,
    /** Concerns a single line item's field; both {@code lineItemId} and {@code fieldName} are set. */
    LINE_ITEM,
    /** Concerns the invoice as a whole and isn't attributable to one field (e.g. a duplicate match). */
    INVOICE
}
