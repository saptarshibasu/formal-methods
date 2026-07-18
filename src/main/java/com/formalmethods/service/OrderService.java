package com.formalmethods.service;

import com.formalmethods.domain.Order;
import com.formalmethods.domain.OrderLifecycle;
import com.formalmethods.domain.OrderStatus;
import com.formalmethods.repository.OrderRepository;
import com.formalmethods.repository.StatusHistoryRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Order lifecycle operations for User Story 1 — create and advance an order
 * one legal step at a time (FR-001/002/003/004/009), admitting only
 * transitions {@link OrderLifecycle} legalizes.
 */
@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public OrderService(OrderRepository orderRepository, StatusHistoryRepository statusHistoryRepository) {
        this.orderRepository = orderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
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
     * leaving the order's current state unchanged.
     */
    public Order applyStatusUpdate(UUID orderId, OrderStatus targetStatus) {
        Order order = findOrThrow(orderId);

        if (!OrderLifecycle.isLegalTransition(order.getStatus(), targetStatus)) {
            LOG.warn("action=applyStatusUpdate order={} from={} to={} outcome=rejected",
                    orderId, order.getStatus(), targetStatus);
            throw new IllegalTransitionException(
                    "illegal transition from " + order.getStatus() + " to " + targetStatus);
        }

        order.setStatus(targetStatus);
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);
        LOG.info("action=applyStatusUpdate order={} to={} outcome=accepted", orderId, targetStatus);
        return saved;
    }

    /** FR-009: returns the order's single current lifecycle state. */
    public Order getOrder(UUID orderId) {
        return findOrThrow(orderId);
    }

    private Order findOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("order not found: " + orderId));
    }
}
