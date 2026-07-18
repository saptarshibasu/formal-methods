package com.formalmethods.verification.service;

/** Thrown when a {@code VerificationJob} id doesn't exist. */
public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(Long jobId) {
        super("Verification job " + jobId + " not found");
    }
}
