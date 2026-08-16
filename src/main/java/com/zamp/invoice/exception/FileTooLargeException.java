package com.zamp.invoice.exception;

/** Thrown when an uploaded file exceeds the configured size limit; maps to HTTP 413. */
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(String message) {
        super(message);
    }
}
