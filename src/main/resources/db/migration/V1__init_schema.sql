-- Initial schema for the verification service.
-- Mirrors com.formalmethods.verification.domain.VerificationJob / VerificationResult.

CREATE TABLE verification_job (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    sensor_type       VARCHAR(32)  NOT NULL,
    source_content    TEXT         NOT NULL,
    source_file_name  VARCHAR(255),
    config_content    TEXT,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    submitted_by      VARCHAR(255),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_verification_job_sensor_type CHECK (sensor_type IN ('LEAN4', 'TLA_PLUS')),
    CONSTRAINT chk_verification_job_status CHECK (status IN ('PENDING', 'RUNNING', 'PASSED', 'FAILED', 'ERROR'))
);

CREATE INDEX idx_verification_job_sensor_type ON verification_job (sensor_type);
CREATE INDEX idx_verification_job_status ON verification_job (status);

CREATE TABLE verification_result (
    id           BIGSERIAL PRIMARY KEY,
    job_id       BIGINT      NOT NULL REFERENCES verification_job (id) ON DELETE CASCADE,
    sensor_type  VARCHAR(32) NOT NULL,
    success      BOOLEAN     NOT NULL,
    summary      VARCHAR(500) NOT NULL,
    raw_output   TEXT,
    exit_code    INTEGER,
    duration_ms  BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_verification_result_sensor_type CHECK (sensor_type IN ('LEAN4', 'TLA_PLUS'))
);

CREATE INDEX idx_verification_result_job_id ON verification_result (job_id);
