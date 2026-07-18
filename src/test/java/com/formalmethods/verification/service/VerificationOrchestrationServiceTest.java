package com.formalmethods.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.formalmethods.verification.domain.JobStatus;
import com.formalmethods.verification.domain.SensorType;
import com.formalmethods.verification.domain.VerificationJob;
import com.formalmethods.verification.repository.VerificationJobRepository;

/**
 * Verifies {@link VerificationOrchestrationService} creates jobs as {@link JobStatus#PENDING}
 * and reads them back by id or filter — it does not run any verification itself; see
 * {@code .agents/agents/lean4-verifier.md} / {@code tlaplus-verifier.md} for that.
 */
class VerificationOrchestrationServiceTest {

    private final VerificationJobRepository jobRepository = mock(VerificationJobRepository.class);
    private final VerificationOrchestrationService service = new VerificationOrchestrationService(jobRepository);

    @Test
    void createJobSavesItAsPending() {
        VerificationJob job = new VerificationJob("t", SensorType.LEAN4, "theorem t : 1 = 1 := rfl", "T.lean", null, "tester");
        when(jobRepository.save(any(VerificationJob.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificationJob created = service.createJob(job);

        assertThat(created.getStatus()).isEqualTo(JobStatus.PENDING);
        verify(jobRepository).save(job);
    }

    @Test
    void getJobReturnsTheStoredJob() {
        VerificationJob job = new VerificationJob("t", SensorType.LEAN4, "theorem t : 1 = 1 := rfl", "T.lean", null, "tester");
        job.setId(1L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThat(service.getJob(1L)).isSameAs(job);
    }

    @Test
    void getJobThrowsJobNotFoundForUnknownId() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJob(99L))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void listJobsFiltersBySensorTypeAndStatusWhenBothGiven() {
        VerificationJob job = new VerificationJob("t", SensorType.TLA_PLUS, "---- MODULE M ----\n====", "M.tla",
                "INVARIANT TypeOK", "tester");
        when(jobRepository.findBySensorTypeAndStatus(SensorType.TLA_PLUS, JobStatus.PENDING))
                .thenReturn(List.of(job));

        assertThat(service.listJobs(SensorType.TLA_PLUS, JobStatus.PENDING)).containsExactly(job);
    }

    @Test
    void listJobsReturnsAllWhenNoFilterGiven() {
        when(jobRepository.findAll()).thenReturn(List.of());

        assertThat(service.listJobs(null, null)).isEmpty();
    }
}
