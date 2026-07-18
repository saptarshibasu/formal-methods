package com.formalmethods.verification.service;

/** Thrown for a request that fails a sensor-specific precondition (e.g. missing TLA+ config). */
public class InvalidVerificationRequestException extends RuntimeException {

    public InvalidVerificationRequestException(String message) {
        super(message);
    }
}
