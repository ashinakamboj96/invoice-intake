package com.zamp.invoice.exception;

import java.util.UUID;

/** Thrown when a requested invoice id doesn't exist; maps to HTTP 404. */
public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super("Invoice not found: " + invoiceId);
    }

    public InvoiceNotFoundException(String message) {
        super(message);
    }
}
