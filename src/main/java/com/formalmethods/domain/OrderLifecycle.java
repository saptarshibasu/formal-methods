package com.formalmethods.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure transition table (legality relation) over {@link OrderStatus} —
 * spec.md's [US1] Formal Verification Obligation and FR-002/003/004/005.
 * Admits exactly the five forward edges NEW..CLOSED and the three cancel
 * edges from {NEW, INVENTORY_RESERVED, PROVISIONED} to CANCELLED, and no
 * others; CLOSED/CANCELLED have no outgoing edge. This class is the Java
 * counterpart to the Lean 4 proof target ({@code Proofs/OrderTransition.lean}
 * per plan.md) — it holds no state and has no side effects.
 */
public final class OrderLifecycle {

    private static final Map<OrderStatus, Set<OrderStatus>> LEGAL_TRANSITIONS = buildTransitionTable();

    private OrderLifecycle() {
    }

    /**
     * True iff moving from {@code from} to {@code to} is one of the exactly
     * eight legal edges the lifecycle admits.
     */
    public static boolean isLegalTransition(OrderStatus from, OrderStatus to) {
        return LEGAL_TRANSITIONS.get(from).contains(to);
    }

    private static Map<OrderStatus, Set<OrderStatus>> buildTransitionTable() {
        Map<OrderStatus, Set<OrderStatus>> table = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            table.put(status, EnumSet.noneOf(OrderStatus.class));
        }
        table.get(OrderStatus.NEW).add(OrderStatus.INVENTORY_RESERVED);
        table.get(OrderStatus.INVENTORY_RESERVED).add(OrderStatus.PROVISIONED);
        table.get(OrderStatus.PROVISIONED).add(OrderStatus.DISPATCHED);
        table.get(OrderStatus.DISPATCHED).add(OrderStatus.DELIVERED);
        table.get(OrderStatus.DELIVERED).add(OrderStatus.CLOSED);

        table.get(OrderStatus.NEW).add(OrderStatus.CANCELLED);
        table.get(OrderStatus.INVENTORY_RESERVED).add(OrderStatus.CANCELLED);
        table.get(OrderStatus.PROVISIONED).add(OrderStatus.CANCELLED);
        return table;
    }
}
