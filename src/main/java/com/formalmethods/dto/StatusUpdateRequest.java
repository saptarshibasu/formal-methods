package com.formalmethods.dto;

import com.formalmethods.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/orders/{orderId}/status}. {@code
 * targetStatus} is deserialized directly into the {@link OrderStatus}
 * allow-list — Jackson rejects any value outside the enum at the boundary,
 * and {@code @NotNull} rejects a missing one (SEC-01/FR-014).
 */
public class StatusUpdateRequest {

    @NotNull
    private OrderStatus targetStatus;

    public OrderStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(OrderStatus targetStatus) {
        this.targetStatus = targetStatus;
    }
}
