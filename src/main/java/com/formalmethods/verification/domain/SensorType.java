package com.formalmethods.verification.domain;

/**
 * Which formal-methods tool a {@link VerificationJob} is checked with.
 *
 * <p>Naming convention: each value here corresponds 1:1 with a
 * {@code .agents/agents/*-verifier.md} agent definition of the same domain
 * (lean4-verifier, tlaplus-verifier) — those coding-assistant agents run the actual
 * check; this service only tracks the job and its persisted result.
 */
public enum SensorType {
    LEAN4,
    TLA_PLUS
}
