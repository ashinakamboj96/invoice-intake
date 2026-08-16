package com.zamp.invoice.exception;

/** Thrown when neither PDF text extraction nor OCR could produce usable text from an uploaded file; maps to HTTP 422. */
public class ExtractionFailedException extends RuntimeException {

    public ExtractionFailedException(String message) {
        super(message);
    }

    public ExtractionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
