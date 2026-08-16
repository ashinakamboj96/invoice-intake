package com.zamp.invoice.enums;

/** Lifecycle state of an uploaded invoice. */
public enum InvoiceStatus {
    /** Extraction/structuring/validation pipeline is still running. */
    PROCESSING,
    /** Passed validation (or a human resolved every failure) with nothing left outstanding. */
    ACCEPTED,
    /** Has at least one unresolved {@code ValidationFailure} awaiting human review. */
    NEEDS_REVIEW,
    /** The pipeline threw before it could produce a reviewable invoice. */
    FAILED,
    /** A human confirmed this invoice as a duplicate; terminal, like {@code ACCEPTED}. */
    REJECTED
}
