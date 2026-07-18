package com.formalmethods.domain;

/**
 * The fixed lifecycle states an order may occupy (spec.md FR-002): the six
 * forward stages NEW..CLOSED plus the terminal CANCELLED branch. Not
 * runtime-configurable — see spec.md's Assumptions.
 */
public enum OrderStatus {
    NEW,
    INVENTORY_RESERVED,
    PROVISIONED,
    DISPATCHED,
    DELIVERED,
    CLOSED,
    CANCELLED
}
