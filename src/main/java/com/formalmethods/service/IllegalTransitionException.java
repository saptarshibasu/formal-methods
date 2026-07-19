package com.formalmethods.service;

/**
 * Thrown when a requested status update or cancel is not a legal transition
 * for the order's current state (FR-003/004/005) — maps to HTTP 409 per
 * plan.md.
 */
public class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(String message) {
        super(message);
    }
}
