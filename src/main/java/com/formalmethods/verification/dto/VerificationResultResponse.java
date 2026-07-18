package com.formalmethods.verification.dto;

import java.time.Instant;

import com.formalmethods.verification.domain.SensorType;
import com.formalmethods.verification.domain.VerificationResult;

public record VerificationResultResponse(
        Long id,
        SensorType sensorType,
        boolean success,
        String summary,
        String rawOutput,
        Integer exitCode,
        long durationMs,
        Instant createdAt
) {
    public static VerificationResultResponse from(VerificationResult result) {
        return new VerificationResultResponse(
                result.getId(),
                result.getSensorType(),
                result.isSuccess(),
                result.getSummary(),
                result.getRawOutput(),
                result.getExitCode(),
                result.getDurationMs(),
                result.getCreatedAt()
        );
    }
}
