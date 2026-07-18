package com.formalmethods.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.formalmethods.domain.Order;
import com.formalmethods.domain.OrderStatus;
import com.formalmethods.repository.OrderRepository;
import com.formalmethods.repository.StatusHistoryRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Service-level tests for User Story 1 (Drive an order through its valid
 * lifecycle). Mocks {@link OrderRepository} and {@link StatusHistoryRepository}
 * at the Spring Data JPA boundary per constitution Article IV — no real or
 * embedded database.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, statusHistoryRepository);
    }

    private static Order orderWithStatus(OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(status);
        order.setInventoryReserved(status == OrderStatus.INVENTORY_RESERVED
                || status == OrderStatus.PROVISIONED);
        return order;
    }

    /**
     * FR-001 / US1: creating an order MUST start it in state NEW.
     * Matters because every downstream transition assumes NEW as the fixed
     * start state — if create ever produced a different starting status the
     * whole ordered lifecycle guarantee would be built on a false premise.
     */
    @Test
    void createOrderStartsInNewState() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order created = orderService.create();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(OrderStatus.NEW);
    }

    /**
     * US1 Acceptance Scenario 1: given a newly created order in NEW, applying
     * an INVENTORY_RESERVED update advances the current state to
     * INVENTORY_RESERVED. Matters because this is the first link in the
     * ordered chain — if this single legal step doesn't work, no forward
     * progress through the lifecycle is possible at all.
     */
    @Test
    void validForwardTransitionFromNewToInventoryReservedAdvancesState() {
        Order order = orderWithStatus(OrderStatus.NEW);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.applyStatusUpdate(orderId, OrderStatus.INVENTORY_RESERVED);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
    }

    /**
     * US1 Acceptance Scenario 1 (extended) / FR-002: every legal forward edge
     * in the fixed sequence NEW -> INVENTORY_RESERVED -> PROVISIONED ->
     * DISPATCHED -> DELIVERED -> CLOSED must be individually accepted and
     * advance the current state to exactly the requested target. Matters
     * because a transition table that skips or mis-wires even one edge would
     * silently break the "one legal step at a time" guarantee for that edge
     * only, which a single-edge test would not catch.
     */
    @ParameterizedTest
    @MethodSource("legalForwardEdges")
    void everyLegalForwardEdgeIsAcceptedAndAdvancesState(OrderStatus from, OrderStatus to) {
        Order order = orderWithStatus(from);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.applyStatusUpdate(orderId, to);

        assertThat(updated.getStatus()).isEqualTo(to);
    }

    private static Stream<Arguments> legalForwardEdges() {
        return Stream.of(
                Arguments.of(OrderStatus.NEW, OrderStatus.INVENTORY_RESERVED),
                Arguments.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.PROVISIONED),
                Arguments.of(OrderStatus.PROVISIONED, OrderStatus.DISPATCHED),
                Arguments.of(OrderStatus.DISPATCHED, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.DELIVERED, OrderStatus.CLOSED));
    }

    /**
     * US1 Acceptance Scenario 2: given an order in NEW, applying a DISPATCHED
     * update (an illegal jump over INVENTORY_RESERVED/PROVISIONED) is
     * rejected and the order's current state remains NEW. Matters because
     * this is the core safety property of the feature (spec.md's [US1]
     * formal obligation) — if an illegal jump were ever silently accepted,
     * an order could skip a lifecycle stage such as inventory reservation.
     */
    @Test
    void illegalJumpFromNewToDispatchedIsRejectedAndStateUnchanged() {
        Order order = orderWithStatus(OrderStatus.NEW);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.applyStatusUpdate(orderId, OrderStatus.DISPATCHED))
                .isInstanceOf(IllegalTransitionException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        verify(orderRepository, org.mockito.Mockito.never()).save(any(Order.class));
    }

    /**
     * US1 Acceptance Scenario 3: given an order in DELIVERED, applying a
     * CLOSED update advances the current state to CLOSED. Matters because
     * CLOSED is the terminal end of the forward chain; if this last edge
     * were mis-wired an order could never legitimately complete its
     * lifecycle.
     */
    @Test
    void validForwardTransitionFromDeliveredToClosedAdvancesState() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.applyStatusUpdate(orderId, OrderStatus.CLOSED);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CLOSED);
    }

    /**
     * US1 Acceptance Scenario 4 (CLOSED branch) / FR-004: given an order in
     * the terminal state CLOSED, any status update is rejected and the state
     * remains CLOSED. Matters because a terminal state that could still be
     * mutated would mean "terminal" is not actually terminal, breaking the
     * spec's [US1] formal obligation that CLOSED has no outgoing transition.
     */
    @Test
    void statusUpdateOnClosedOrderIsRejectedAndStateUnchanged() {
        Order order = orderWithStatus(OrderStatus.CLOSED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.applyStatusUpdate(orderId, OrderStatus.INVENTORY_RESERVED))
                .isInstanceOf(IllegalTransitionException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CLOSED);
        verify(orderRepository, org.mockito.Mockito.never()).save(any(Order.class));
    }

    /**
     * US1 Acceptance Scenario 4 (CANCELLED branch) / FR-004: given an order
     * in the terminal state CANCELLED, any status update is rejected and the
     * state remains CANCELLED. Matters because CANCELLED is the feature's
     * other terminal state and must be equally immune to further mutation —
     * testing only CLOSED would leave this half of FR-004 unverified.
     */
    @Test
    void statusUpdateOnCancelledOrderIsRejectedAndStateUnchanged() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.applyStatusUpdate(orderId, OrderStatus.INVENTORY_RESERVED))
                .isInstanceOf(IllegalTransitionException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, org.mockito.Mockito.never()).save(any(Order.class));
    }

    /**
     * US1 Acceptance Scenario 5 / FR-009: given any existing order, requesting
     * its current state returns the order's single current lifecycle state.
     * Matters because read-state is the only way US1's other scenarios are
     * externally observable — if it returned something other than the true
     * current status, every other guarantee in this story would be
     * unverifiable from outside the service.
     */
    @Test
    void getOrderReturnsCurrentLifecycleState() {
        Order order = orderWithStatus(OrderStatus.PROVISIONED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order fetched = orderService.getOrder(orderId);

        assertThat(fetched.getStatus()).isEqualTo(OrderStatus.PROVISIONED);
    }
}
