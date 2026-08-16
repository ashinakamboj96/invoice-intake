package com.zamp.invoice.exception;

/** Thrown when the LLM provider call fails after all retries, or returns unparseable output; maps to HTTP 503. */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
