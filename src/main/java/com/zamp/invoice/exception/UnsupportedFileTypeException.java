package com.zamp.invoice.exception;

/** Thrown when an uploaded file's content type isn't one of the accepted invoice formats; maps to HTTP 415. */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
