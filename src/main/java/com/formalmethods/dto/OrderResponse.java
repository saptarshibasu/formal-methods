package com.formalmethods.dto;

import com.formalmethods.domain.Order;
import com.formalmethods.domain.OrderStatus;
import java.util.UUID;

/** Client-facing view of an {@link Order} (plan.md). */
public class OrderResponse {

    private final UUID id;
    private final OrderStatus status;
    private final boolean inventoryReserved;

    public OrderResponse(UUID id, OrderStatus status, boolean inventoryReserved) {
        this.id = id;
        this.status = status;
        this.inventoryReserved = inventoryReserved;
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.isInventoryReserved());
    }

    public UUID getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public boolean isInventoryReserved() {
        return inventoryReserved;
    }
}
