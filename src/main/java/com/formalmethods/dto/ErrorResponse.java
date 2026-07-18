package com.formalmethods.dto;

/**
 * Generic client-facing error body (SEC-05/SEC-07, FR-017) — carries no
 * internal detail (stack traces, entity ids beyond what the caller already
 * supplied, exception class names). Full context is logged server-side
 * instead.
 */
public class ErrorResponse {

    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
