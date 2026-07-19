package com.formalmethods.service;

/**
 * Thrown when a status update or cancel loses a concurrent optimistic-lock
 * race on the order's {@code @Version}-guarded row (FR-013) — maps to HTTP
 * 409 per plan.md.
 */
public class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
