package com.formalmethods.verification.dto;

import com.formalmethods.verification.domain.SensorType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/verification/jobs}.
 *
 * @param name           short human-readable label for the job
 * @param sensorType     which feedback sensor should check {@code sourceContent}
 * @param sourceContent  the .lean or .tla source to verify
 * @param sourceFileName optional file name, used for diagnostics and (for TLA+) to
 *                        derive the matching .cfg's base name
 * @param configContent  TLA+ .cfg contents — required when {@code sensorType} is
 *                        {@code TLA_PLUS}, ignored for {@code LEAN4}
 * @param submittedBy    optional identifier of who/what submitted the job
 */
public record CreateVerificationJobRequest(
        @NotBlank String name,
        @NotNull SensorType sensorType,
        @NotBlank String sourceContent,
        String sourceFileName,
        String configContent,
        String submittedBy
) {
}
