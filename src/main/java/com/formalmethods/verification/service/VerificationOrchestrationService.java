package com.formalmethods.verification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.formalmethods.verification.domain.JobStatus;
import com.formalmethods.verification.domain.SensorType;
import com.formalmethods.verification.domain.VerificationJob;
import com.formalmethods.verification.repository.VerificationJobRepository;

/**
 * Creates and queries {@link VerificationJob}s. Actual verification (running Lean 4
 * or TLC against a job's submitted source) happens outside this service, via the
 * framework's {@code lean4-verifier}/{@code tlaplus-verifier} coding-assistant
 * agents — this service only tracks jobs and their persisted results.
 */
@Service
public class VerificationOrchestrationService {

    private final VerificationJobRepository jobRepository;

    public VerificationOrchestrationService(VerificationJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public VerificationJob createJob(VerificationJob job) {
        job.setStatus(JobStatus.PENDING);
        return jobRepository.save(job);
    }

    public VerificationJob getJob(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    public List<VerificationJob> listJobs(SensorType sensorType, JobStatus status) {
        if (sensorType != null && status != null) {
            return jobRepository.findBySensorTypeAndStatus(sensorType, status);
        }
        if (sensorType != null) {
            return jobRepository.findBySensorType(sensorType);
        }
        if (status != null) {
            return jobRepository.findByStatus(status);
        }
        return jobRepository.findAll();
    }
}
