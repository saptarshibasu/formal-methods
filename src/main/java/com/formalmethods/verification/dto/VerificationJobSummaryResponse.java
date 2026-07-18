package com.formalmethods.verification.dto;

import java.time.Instant;

import com.formalmethods.verification.domain.JobStatus;
import com.formalmethods.verification.domain.SensorType;
import com.formalmethods.verification.domain.VerificationJob;

/** Lightweight job listing row — no raw tool output, unlike {@link VerificationJobResponse}. */
public record VerificationJobSummaryResponse(
        Long id,
        String name,
        SensorType sensorType,
        JobStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static VerificationJobSummaryResponse from(VerificationJob job) {
        return new VerificationJobSummaryResponse(
                job.getId(),
                job.getName(),
                job.getSensorType(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
