package com.formalmethods.dto;

import com.formalmethods.domain.OrderStatus;
import com.formalmethods.domain.StatusHistoryEntry;
import java.time.Instant;

/** Client-facing view of one {@link StatusHistoryEntry} (plan.md, FR-010). */
public class HistoryEntryResponse {

    private final OrderStatus status;
    private final Instant changedAt;

    public HistoryEntryResponse(OrderStatus status, Instant changedAt) {
        this.status = status;
        this.changedAt = changedAt;
    }

    public static HistoryEntryResponse from(StatusHistoryEntry entry) {
        return new HistoryEntryResponse(entry.getStatus(), entry.getChangedAt());
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
