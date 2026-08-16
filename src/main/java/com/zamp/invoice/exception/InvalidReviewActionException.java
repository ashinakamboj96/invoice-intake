package com.zamp.invoice.exception;

/** Thrown when a review submission is invalid: wrong invoice status, an uncovered failure, or a missing/invalid correction value; maps to HTTP 400. */
public class InvalidReviewActionException extends RuntimeException {

    public InvalidReviewActionException(String message) {
        super(message);
    }
}
