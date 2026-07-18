package com.formalmethods.verification.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A unit of formal-methods work submitted for verification: a Lean 4 proof file's
 * contents, or a TLA+ spec's contents, along with which {@link SensorType} tool
 * should check it.
 *
 * <p>A job accumulates one {@link VerificationResult} per run — re-running the same
 * job (e.g. after the proof/spec was edited and resubmitted) appends a new result
 * rather than overwriting the last one, so the job's history of feedback is preserved.
 */
@Entity
@Table(name = "verification_job")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "results")
public class VerificationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 32)
    private SensorType sensorType;

    /** Full contents of the .lean or .tla file submitted for checking. */
    @Column(name = "source_content", nullable = false, columnDefinition = "text")
    private String sourceContent;

    /** Optional file name (e.g. "OrderTotal.lean", "OrderSpec.tla") for diagnostics. */
    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    /**
     * TLA+ .cfg contents (INVARIANT/PROPERTY/CONSTANTS) — required for
     * {@link SensorType#TLA_PLUS} jobs, since TLC cannot model-check a spec without
     * its matching config. Unused for {@link SensorType#LEAN4} jobs.
     */
    @Column(name = "config_content", columnDefinition = "text")
    private String configContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "submitted_by", length = 255)
    private String submittedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // EAGER + @BatchSize rather than LAZY: results are a small, bounded-size list per
    // job (one per run), and the REST layer (VerificationJobResponse) always needs
    // them once the request layer's Hibernate session has already closed
    // (open-in-view is disabled) — LAZY here would mean either a
    // LazyInitializationException on the ERROR path (a job with a result collection
    // never otherwise touched) or a JOIN FETCH query for every read path. @BatchSize
    // keeps a job-list response to one extra `IN (...)` query instead of N+1.
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    @org.hibernate.annotations.BatchSize(size = 20)
    private List<VerificationResult> results = new ArrayList<>();

    public VerificationJob(String name, SensorType sensorType, String sourceContent, String sourceFileName,
                            String configContent, String submittedBy) {
        this.name = name;
        this.sensorType = sensorType;
        this.sourceContent = sourceContent;
        this.sourceFileName = sourceFileName;
        this.configContent = configContent;
        this.submittedBy = submittedBy;
        this.status = JobStatus.PENDING;
    }

    public void addResult(VerificationResult result) {
        result.setJob(this);
        this.results.add(result);
        this.status = result.isSuccess() ? JobStatus.PASSED : JobStatus.FAILED;
    }
}
