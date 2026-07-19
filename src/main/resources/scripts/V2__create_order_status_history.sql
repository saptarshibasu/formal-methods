-- V2: order_status_history table.
-- Append-only audit trail of every accepted status change (FR-007/008).
-- No UPDATE/DELETE path is exposed anywhere in this feature; chronological
-- order is the monotonic identity `id`.
CREATE TABLE order_status_history (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES orders(id),
    status      VARCHAR(32) NOT NULL,
    changed_at  TIMESTAMP NOT NULL
);
