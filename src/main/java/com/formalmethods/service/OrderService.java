package com.formalmethods.service;

import com.formalmethods.domain.Order;
import com.formalmethods.domain.OrderLifecycle;
import com.formalmethods.domain.OrderStatus;
import com.formalmethods.domain.StatusHistoryEntry;
import com.formalmethods.repository.OrderRepository;
import com.formalmethods.repository.StatusHistoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order lifecycle operations for User Story 1 — create and advance an order
 * one legal step at a time (FR-001/002/003/004/009), admitting only
 * transitions {@link OrderLifecycle} legalizes — User Story 2 — cancel
 * an order with an exactly-once inventory release (FR-005/006) — and User
 * Story 3 — an append-only history that never diverges from current state
 * (FR-007/008/010/011).
 */
@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final InventoryReleaseClient inventoryReleaseClient;

    public OrderService(OrderRepository orderRepository, StatusHistoryRepository statusHistoryRepository,
            InventoryReleaseClient inventoryReleaseClient) {
        this.orderRepository = orderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.inventoryReleaseClient = inventoryReleaseClient;
    }

    /** FR-001: creates a new order, starting in state NEW. */
    public Order create() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.NEW);
        order.setInventoryReserved(false);
        Instant now = Instant.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        Order saved = orderRepository.save(order);
        LOG.info("action=create order={} outcome=accepted resultStatus={}", saved.getId(), saved.getStatus());
        return saved;
    }

    /**
     * FR-003/004: applies a status update only when {@code targetStatus} is
     * the legal next state for the order's current state; otherwise rejects,
     * leaving the order's current state unchanged. FR-011: a target equal to
     * the order's current status is accepted as an idempotent no-op — no
     * state change, no history entry, no repeated side effect. FR-007/008:
     * an accepted transition updates state and appends exactly one history
     * entry atomically, so the two can never diverge, even across a crash or
     * retry.
     */
    @Transactional
    public Order applyStatusUpdate(UUID orderId, OrderStatus targetStatus) {
        Order order = findOrThrow(orderId);

        if (order.getStatus() == targetStatus) {
            LOG.info("action=applyStatusUpdate order={} to={} outcome=accepted(idempotent)", orderId, targetStatus);
            return order;
        }

        if (!OrderLifecycle.isLegalTransition(order.getStatus(), targetStatus)) {
            LOG.warn("action=applyStatusUpdate order={} from={} to={} outcome=rejected",
                    orderId, order.getStatus(), targetStatus);
            throw new IllegalTransitionException(
                    "illegal transition from " + order.getStatus() + " to " + targetStatus);
        }

        order.setStatus(targetStatus);
        Instant now = Instant.now();
        order.setUpdatedAt(now);
        Order saved = orderRepository.save(order);
        statusHistoryRepository.save(new StatusHistoryEntry(orderId, targetStatus, now));
        LOG.info("action=applyStatusUpdate order={} to={} outcome=accepted", orderId, targetStatus);
        return saved;
    }

    /** FR-009: returns the order's single current lifecycle state. */
    public Order getOrder(UUID orderId) {
        return findOrThrow(orderId);
    }

    /** FR-010: returns the order's status history in chronological order. */
    public List<StatusHistoryEntry> getHistory(UUID orderId) {
        findOrThrow(orderId);
        return statusHistoryRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    /**
     * FR-005/006: cancels an order at any point strictly before DISPATCHED,
     * releasing inventory reserved at INVENTORY_RESERVED or later
     * (pre-DISPATCHED) exactly once. A repeat cancel on an already-CANCELLED
     * order is an idempotent no-op — no second release, no exception
     * (FR-011, spec.md Edge Cases). A cancel at or after DISPATCHED is
     * rejected, leaving the order's current state unchanged. FR-007/008: an
     * accepted cancel updates state and appends exactly one history entry
     * atomically, so the two can never diverge, even across a crash or retry.
     */
    @Transactional
    public Order cancel(UUID orderId) {
        Order order = findOrThrow(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            LOG.info("action=cancel order={} outcome=accepted(idempotent) resultStatus=CANCELLED", orderId);
            return order;
        }

        if (!OrderLifecycle.isLegalTransition(order.getStatus(), OrderStatus.CANCELLED)) {
            LOG.warn("action=cancel order={} from={} outcome=rejected", orderId, order.getStatus());
            throw new IllegalTransitionException(
                    "illegal cancel from " + order.getStatus());
        }

        boolean shouldReleaseInventory = order.getStatus() == OrderStatus.INVENTORY_RESERVED
                || order.getStatus() == OrderStatus.PROVISIONED;

        order.setStatus(OrderStatus.CANCELLED);
        Instant now = Instant.now();
        order.setUpdatedAt(now);
        Order saved = orderRepository.save(order);
        statusHistoryRepository.save(new StatusHistoryEntry(orderId, OrderStatus.CANCELLED, now));

        if (shouldReleaseInventory) {
            inventoryReleaseClient.release(orderId);
            LOG.info("action=cancel order={} outcome=accepted resultStatus=CANCELLED inventoryReleased=true",
                    orderId);
        } else {
            LOG.info("action=cancel order={} outcome=accepted resultStatus=CANCELLED inventoryReleased=false",
                    orderId);
        }

        return saved;
    }

    private Order findOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("order not found: " + orderId));
    }
}
