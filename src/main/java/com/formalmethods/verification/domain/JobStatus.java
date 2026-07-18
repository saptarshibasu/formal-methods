package com.formalmethods.verification.domain;

/** Lifecycle state of a {@link VerificationJob}. */
public enum JobStatus {
    PENDING,
    RUNNING,
    PASSED,
    FAILED,
    ERROR
}
