package com.formalmethods.verification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the verification service.
 *
 * <p>The service persists {@link com.formalmethods.verification.domain.VerificationJob}s
 * (a Lean 4 proof or a TLA+ spec submitted for checking) to PostgreSQL via Spring Data
 * JPA. Actual verification runs outside this service, via the framework's
 * {@code lean4-verifier}/{@code tlaplus-verifier} coding-assistant agents.
 */
@SpringBootApplication
public class VerificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VerificationServiceApplication.class, args);
    }
}
