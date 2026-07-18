package com.formalmethods.verification.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.formalmethods.verification.domain.JobStatus;
import com.formalmethods.verification.domain.SensorType;
import com.formalmethods.verification.domain.VerificationJob;
import com.formalmethods.verification.dto.CreateVerificationJobRequest;
import com.formalmethods.verification.dto.VerificationJobResponse;
import com.formalmethods.verification.dto.VerificationJobSummaryResponse;
import com.formalmethods.verification.service.InvalidVerificationRequestException;
import com.formalmethods.verification.service.VerificationOrchestrationService;

import jakarta.validation.Valid;

/**
 * REST surface for submitting formal-methods verification jobs (Lean 4 proofs,
 * TLA+ specs) and reading back the feedback sensors' results.
 */
@RestController
@RequestMapping("/api/verification/jobs")
public class VerificationController {

    private final VerificationOrchestrationService orchestrationService;

    public VerificationController(VerificationOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public ResponseEntity<VerificationJobResponse> createJob(@Valid @RequestBody CreateVerificationJobRequest request) {
        if (request.sensorType() == SensorType.TLA_PLUS
                && (request.configContent() == null || request.configContent().isBlank())) {
            throw new InvalidVerificationRequestException(
                    "configContent is required when sensorType is TLA_PLUS (TLC needs a matching .cfg)");
        }

        VerificationJob job = new VerificationJob(
                request.name(),
                request.sensorType(),
                request.sourceContent(),
                request.sourceFileName(),
                request.configContent(),
                request.submittedBy()
        );
        VerificationJob created = orchestrationService.createJob(job);

        return ResponseEntity
                .created(URI.create("/api/verification/jobs/" + created.getId()))
                .body(VerificationJobResponse.from(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VerificationJobResponse> getJob(@PathVariable Long id) {
        VerificationJob job = orchestrationService.getJob(id);
        return ResponseEntity.ok(VerificationJobResponse.from(job));
    }

    @GetMapping
    public ResponseEntity<List<VerificationJobSummaryResponse>> listJobs(
            @RequestParam(required = false) SensorType sensorType,
            @RequestParam(required = false) JobStatus status) {
        List<VerificationJobSummaryResponse> jobs = orchestrationService.listJobs(sensorType, status).stream()
                .map(VerificationJobSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(jobs);
    }
}
