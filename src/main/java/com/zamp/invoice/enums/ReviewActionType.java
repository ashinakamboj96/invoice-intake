package com.zamp.invoice.enums;

/** The action a human reviewer took to resolve a single {@code ValidationFailure}. */
public enum ReviewActionType {
    /** Reviewer looked at the flagged value and confirmed it's correct as-is. */
    APPROVED,
    /** Reviewer supplied a replacement value, applied to the invoice/line item field. */
    CORRECTED,
    /** Reviewer confirmed an {@code EXACT_DUPLICATE} match; the invoice is rejected. */
    DUPLICATE_CONFIRMED,
    /** Reviewer confirmed this is NOT a duplicate of the flagged related invoice. */
    DUPLICATE_DISMISSED
}
