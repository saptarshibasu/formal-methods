package com.formalmethods.verification.dto;

import java.time.Instant;
import java.util.List;

import com.formalmethods.verification.domain.JobStatus;
import com.formalmethods.verification.domain.SensorType;
import com.formalmethods.verification.domain.VerificationJob;

public record VerificationJobResponse(
        Long id,
        String name,
        SensorType sensorType,
        String sourceFileName,
        JobStatus status,
        String submittedBy,
        Instant createdAt,
        Instant updatedAt,
        List<VerificationResultResponse> results
) {
    public static VerificationJobResponse from(VerificationJob job) {
        return new VerificationJobResponse(
                job.getId(),
                job.getName(),
                job.getSensorType(),
                job.getSourceFileName(),
                job.getStatus(),
                job.getSubmittedBy(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getResults().stream().map(VerificationResultResponse::from).toList()
        );
    }
}
