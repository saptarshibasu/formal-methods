package com.formalmethods.service;

/**
 * Thrown when a status update, cancel, or read references an order id that
 * does not exist (FR-015) — maps to HTTP 404 per plan.md.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
