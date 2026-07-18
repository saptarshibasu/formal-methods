package com.formalmethods.verification.domain;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * One feedback reading for a single {@link VerificationJob} run — the tool's
 * verdict (pass/fail), its one-line summary, and its raw output for diagnostics.
 */
@Entity
@Table(name = "verification_result")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "job")
public class VerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private VerificationJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 32)
    private SensorType sensorType;

    @Column(nullable = false)
    private boolean success;

    /** One-line human-readable verdict, e.g. "verified" or "unresolved goal at line 42". */
    @Column(nullable = false, length = 500)
    private String summary;

    /** Full stdout/stderr captured from the underlying `lean`/`lake` or TLC invocation. */
    @Column(name = "raw_output", columnDefinition = "text")
    private String rawOutput;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "duration_ms")
    private long durationMs;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public VerificationResult(SensorType sensorType, boolean success, String summary,
                               String rawOutput, Integer exitCode, long durationMs) {
        this.sensorType = sensorType;
        this.success = success;
        this.summary = summary;
        this.rawOutput = rawOutput;
        this.exitCode = exitCode;
        this.durationMs = durationMs;
    }
}
