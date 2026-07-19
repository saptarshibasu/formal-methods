-- V1: orders table.
-- Holds the current lifecycle state of a single order plus the optimistic
-- lock (`version`) that arbitrates concurrent writers (FR-013).
CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    status              VARCHAR(32) NOT NULL,
    inventory_reserved  BOOLEAN NOT NULL,
    version             BIGINT NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);
