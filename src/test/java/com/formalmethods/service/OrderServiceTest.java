package com.formalmethods.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.formalmethods.domain.Order;
import com.formalmethods.domain.OrderStatus;
import com.formalmethods.domain.StatusHistoryEntry;
import com.formalmethods.repository.OrderRepository;
import com.formalmethods.repository.StatusHistoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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

    @Mock
    private InventoryReleaseClient inventoryReleaseClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, statusHistoryRepository, inventoryReleaseClient);
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
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

    /**
     * US2 Acceptance Scenario 1 / FR-005/FR-006: given an order in NEW,
     * cancelling it moves the current state to CANCELLED and does NOT invoke
     * the inventory release client. Matters because inventory was never
     * reserved for a NEW order — an unconditional release call here would
     * be a spurious external side effect against a reservation that does
     * not exist.
     */
    @Test
    void cancelOrderInNewStateMovesToCancelledWithoutInventoryRelease() {
        Order order = orderWithStatus(OrderStatus.NEW);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = orderService.cancel(orderId);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryReleaseClient, never()).release(any(UUID.class));
    }

    /**
     * US2 Acceptance Scenario 2 / FR-005/FR-006: given an order that has
     * reached INVENTORY_RESERVED or PROVISIONED (inventory already
     * reserved), cancelling it moves the current state to CANCELLED and
     * invokes the inventory release client exactly once. Matters because
     * FR-006 requires the release call for every pre-DISPATCHED state at or
     * after INVENTORY_RESERVED, not just the first one — missing the
     * PROVISIONED case would leave reserved stock silently un-released.
     */
    @ParameterizedTest
    @MethodSource("qualifyingCancelStates")
    void cancelOrderWithReservedInventoryMovesToCancelledAndReleasesExactlyOnce(OrderStatus from) {
        Order order = orderWithStatus(from);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = orderService.cancel(orderId);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryReleaseClient, times(1)).release(orderId);
    }

    private static Stream<Arguments> qualifyingCancelStates() {
        return Stream.of(
                Arguments.of(OrderStatus.INVENTORY_RESERVED),
                Arguments.of(OrderStatus.PROVISIONED));
    }

    /**
     * US2 Acceptance Scenario 3 / FR-005: given an order in DISPATCHED,
     * requesting cancellation is rejected and the state remains DISPATCHED,
     * with no inventory release invoked. Matters because DISPATCHED is the
     * spec's explicit cutoff ("cancellation at or after DISPATCHED MUST be
     * rejected") — accepting it here would let an order already in the
     * courier's hands be silently cancelled after the fact.
     */
    @Test
    void cancelOrderInDispatchedIsRejectedAndStateUnchanged() {
        Order order = orderWithStatus(OrderStatus.DISPATCHED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(orderId))
                .isInstanceOf(IllegalTransitionException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DISPATCHED);
        verify(orderRepository, never()).save(any(Order.class));
        verify(inventoryReleaseClient, never()).release(any(UUID.class));
    }

    /**
     * US2 Acceptance Scenario 4 / FR-011 (idempotency edge case): given an
     * order already in CANCELLED, requesting cancellation again leaves the
     * state at CANCELLED, does not throw, and does not invoke a second
     * inventory release. Matters because spec.md's Edge Cases explicitly
     * calls out "a cancel arriving after the order is already CANCELLED...
     * never re-releasing inventory" — without this guarantee a re-delivered
     * cancel request could double-release inventory that was already freed.
     */
    @Test
    void cancelAlreadyCancelledOrderIsIdempotentWithNoSecondRelease() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.cancel(orderId);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryReleaseClient, never()).release(any(UUID.class));
    }

    /**
     * US3 Acceptance Scenario 1 / FR-007/FR-008: a valid, accepted status
     * update appends exactly one {@link StatusHistoryEntry} for the order,
     * recording the resulting status, and the order's current state equals
     * that entry's state. Matters because an accepted transition that
     * doesn't append a history entry (or appends the wrong status) would let
     * the audit trail silently diverge from reality on the very first write
     * — exactly the divergence FR-008 forbids.
     */
    @Test
    void validStatusUpdateAppendsExactlyOneHistoryEntryMatchingCurrentState() {
        Order order = orderWithStatus(OrderStatus.NEW);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statusHistoryRepository.save(any(StatusHistoryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.applyStatusUpdate(orderId, OrderStatus.INVENTORY_RESERVED);

        ArgumentCaptor<StatusHistoryEntry> captor = ArgumentCaptor.forClass(StatusHistoryEntry.class);
        verify(statusHistoryRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        assertThat(updated.getStatus()).isEqualTo(captor.getValue().getStatus());
    }

    /**
     * US3 Acceptance Scenario 2 / FR-007: a status update rejected as an
     * illegal transition appends NO history entry, and the prior current
     * state is retained. Matters because FR-007 explicitly forbids a
     * rejected update from creating a history entry — if a rejected jump
     * still wrote to history, the audit trail would record a transition that
     * never actually took effect, itself a divergence from current state.
     */
    @Test
    void illegalStatusUpdateAppendsNoHistoryEntry() {
        Order order = orderWithStatus(OrderStatus.NEW);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.applyStatusUpdate(orderId, OrderStatus.DISPATCHED))
                .isInstanceOf(IllegalTransitionException.class);

        verify(statusHistoryRepository, never()).save(any());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
    }

    /**
     * US3 Acceptance Scenario 3 / FR-011: re-delivering a status update whose
     * target equals the order's current status (a retry/duplicate of an
     * already-applied update) is accepted as a no-op — current state and
     * history remain exactly as after the first application: no second
     * history entry, no repeated persistence side effect. Matters because
     * without this idempotent handling a re-delivered "advance to X" (at-
     * least-once delivery, spec.md's Edge Cases) would either be wrongly
     * rejected as illegal (no self-loop edge exists in {@code
     * OrderLifecycle}) or, worse, silently double-append history — either
     * way violating FR-011's "no additional state change, no additional
     * history entry" guarantee.
     */
    @Test
    void reDeliveredUpdateToCurrentStatusIsAcceptedAsNoOpWithNoNewHistoryEntry() {
        Order order = orderWithStatus(OrderStatus.INVENTORY_RESERVED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.applyStatusUpdate(orderId, OrderStatus.INVENTORY_RESERVED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        verify(statusHistoryRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    /**
     * Pins the human-decided precedence between FR-011 (duplicate-delivery
     * idempotency) and US1 Scenario 4 ("any status update on a terminal
     * order is rejected"): a self-target duplicate on CLOSED (CLOSED ->
     * CLOSED) is accepted as a no-op, not rejected. Matters because the
     * idempotency check in {@code applyStatusUpdate} runs unconditionally
     * before the legality check, so a re-delivered update whose target
     * equals CLOSED never actually moves the order anywhere — treating it
     * as a no-op is harmless and consistent with FR-011's general rule. A
     * genuine transition attempt to a *different* target on CLOSED remains
     * rejected via the legality check, unaffected by this test (see
     * statusUpdateOnClosedOrderIsRejectedAndStateUnchanged above).
     */
    @Test
    void applyStatusUpdateWithSelfTargetOnClosedOrderIsAcceptedAsNoOp() {
        Order order = orderWithStatus(OrderStatus.CLOSED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.applyStatusUpdate(orderId, OrderStatus.CLOSED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CLOSED);
        verify(orderRepository, never()).save(any(Order.class));
        verify(statusHistoryRepository, never()).save(any());
    }

    /**
     * Pins the same FR-011-over-Scenario-4 precedence decision for the
     * CANCELLED terminal state: a self-target duplicate (CANCELLED ->
     * CANCELLED) is accepted as a no-op, not rejected. Matters for the same
     * reason as the CLOSED case above — a self-target duplicate never moves
     * the order, so it's harmless even on a terminal state; testing only
     * CLOSED would leave this half of the precedence decision unverified.
     */
    @Test
    void applyStatusUpdateWithSelfTargetOnCancelledOrderIsAcceptedAsNoOp() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.applyStatusUpdate(orderId, OrderStatus.CANCELLED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, never()).save(any(Order.class));
        verify(statusHistoryRepository, never()).save(any());
    }

    /**
     * US3 Acceptance Scenario 4 / FR-010: requesting an order's history
     * returns its entries in chronological order, and the last entry's
     * status equals the order's current state. Matters because
     * chronological order is what makes the history usable as an audit
     * trail at all — an out-of-order or incomplete read would make it
     * impossible for a caller to verify "current state agrees with the
     * latest entry" (FR-008), which is this story's headline guarantee.
     */
    @Test
    void getHistoryReturnsEntriesInChronologicalOrderMatchingCurrentState() {
        Order order = orderWithStatus(OrderStatus.PROVISIONED);
        UUID orderId = order.getId();
        Instant t1 = Instant.parse("2026-07-18T00:00:00Z");
        Instant t2 = Instant.parse("2026-07-18T00:05:00Z");
        StatusHistoryEntry first = new StatusHistoryEntry(orderId, OrderStatus.INVENTORY_RESERVED, t1);
        StatusHistoryEntry second = new StatusHistoryEntry(orderId, OrderStatus.PROVISIONED, t2);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(statusHistoryRepository.findByOrderIdOrderByIdAsc(orderId)).thenReturn(List.of(first, second));

        List<StatusHistoryEntry> history = orderService.getHistory(orderId);

        assertThat(history).containsExactly(first, second);
        assertThat(history.get(history.size() - 1).getStatus()).isEqualTo(order.getStatus());
    }

    /**
     * US4 Acceptance Scenario 1 / FR-011 (T030a): given an order already in
     * INVENTORY_RESERVED, delivering a duplicate INVENTORY_RESERVED update
     * (simulating an upstream warehouse system's at-least-once redelivery,
     * not merely a generic client retry) produces no additional state change
     * and no additional history entry — no {@code orderRepository.save} and
     * no {@code statusHistoryRepository.save} call at all. Matters because
     * US4 frames this as a distinct at-least-once-delivery correctness
     * requirement (spec.md's "Why this priority") separate from US3's
     * general idempotency mechanism — if the two ever diverged (e.g. a
     * future change special-cased "retry" detection instead of relying on
     * the target-equals-current-status no-op), a redelivered upstream
     * duplicate could silently double-apply a side effect that a bare
     * client-retry test would never exercise.
     */
    @Test
    void duplicateInventoryReservedDeliveryFromUpstreamProducesNoAdditionalEffect() {
        Order order = orderWithStatus(OrderStatus.INVENTORY_RESERVED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.applyStatusUpdate(orderId, OrderStatus.INVENTORY_RESERVED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        verify(orderRepository, never()).save(any(Order.class));
        verify(statusHistoryRepository, never()).save(any());
    }

    /**
     * US4 Acceptance Scenario 2 / FR-012 (T030b): given an order in NEW, a
     * PROVISIONED update arriving before the INVENTORY_RESERVED update it
     * depends on (an out-of-order upstream delivery — e.g. provisioning's
     * callback racing ahead of the warehouse's) is rejected, and the order
     * stays in NEW. Matters because US4 explicitly calls this out as its own
     * acceptance scenario (delayed/out-of-order upstream delivery, not just
     * "any illegal jump" from US1) — if a future change relaxed the
     * transition table to tolerate "catch-up" jumps for convenience, this
     * test pins that a premature update must still be rejected outright
     * rather than silently reordered or accepted.
     */
    @Test
    void prematureProvisionedUpdateBeforeInventoryReservedIsRejectedAndOrderStaysNew() {
        Order order = orderWithStatus(OrderStatus.NEW);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.applyStatusUpdate(orderId, OrderStatus.PROVISIONED))
                .isInstanceOf(IllegalTransitionException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        verify(orderRepository, never()).save(any(Order.class));
        verify(statusHistoryRepository, never()).save(any());
    }

    /**
     * US4 Acceptance Scenario 3 / FR-013 (T031): given an order in
     * PROVISIONED, a DISPATCHED status update racing a concurrent cancel on
     * a different service instance is modeled by having the mocked
     * {@link OrderRepository#save} throw
     * {@link ObjectOptimisticLockingFailureException} on the losing write
     * (the shared row's {@code @Version} column is the arbiter per plan.md —
     * whichever writer commits first wins, the other's save fails this way).
     * The losing {@code applyStatusUpdate(DISPATCHED)} call MUST surface as
     * a domain-level {@link ConcurrentUpdateException} — not the raw Spring
     * exception propagating uncaught — and MUST NOT have appended a history
     * entry. Matters because FR-013 requires that of two conflicting
     * concurrent updates at most one is accepted, with history and current
     * state remaining mutually consistent afterward; an uncaught
     * {@code ObjectOptimisticLockingFailureException} leaking past
     * {@code OrderService} would (a) not map to any defined client-facing
     * rejection (breaking plan.md's "maps to HTTP 409" design) and (b) risk
     * a partially-applied write if a caller mistakenly treated the raw
     * Spring exception as retryable without knowing the transition never
     * took effect.
     *
     * <p>Exception-naming decision for implementor (T032): this test
     * expects a new {@code com.formalmethods.service.ConcurrentUpdateException}
     * (a {@code RuntimeException}, sibling to {@link IllegalTransitionException})
     * that {@code OrderService} throws after catching
     * {@code ObjectOptimisticLockingFailureException} from the repository
     * save — it does not yet exist, so this test is red on a missing-symbol
     * compile error until T032 introduces it.
     */
    @Test
    void concurrentDispatchedUpdateLosingOptimisticLockRaceIsRejectedNotRawSpringException() {
        Order order = orderWithStatus(OrderStatus.PROVISIONED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Order.class, orderId));

        assertThatThrownBy(() -> orderService.applyStatusUpdate(orderId, OrderStatus.DISPATCHED))
                .isInstanceOf(ConcurrentUpdateException.class);

        verify(statusHistoryRepository, never()).save(any());
    }

    /**
     * [US2/US3/US4] Formal Verification Obligation / FR-006: a bug found by
     * {@code tlaplus-spec-writer} while modeling the invariant "inventory is
     * released exactly once, and only via the dedicated cancel path" —
     * {@code applyStatusUpdate(orderId, CANCELLED)} against an order at
     * INVENTORY_RESERVED (inventory already reserved) MUST be rejected with
     * {@link IllegalTransitionException}, leaving the order's state
     * unchanged, appending no history entry, and — the crux of the bug —
     * never invoking {@link InventoryReleaseClient#release}. Matters because
     * {@code OrderLifecycle.isLegalTransition} still legally admits
     * INVENTORY_RESERVED/PROVISIONED -> CANCELLED (needed for the dedicated
     * {@code /cancel} endpoint), so today nothing in {@code
     * applyStatusUpdate} excludes CANCELLED as a status-update target: the
     * generic status-update path silently moves the order to CANCELLED
     * without ever calling the release client, corrupting the exactly-once
     * inventory-release guarantee that only {@code OrderService.cancel}
     * currently upholds. Without this rejection, reserved inventory is
     * leaked (never released, never re-reservable) for every order cancelled
     * through {@code POST /status} instead of {@code POST /cancel}.
     */
    @Test
    void applyStatusUpdateToCancelledIsRejectedAndNeverReleasesInventory() {
        Order order = orderWithStatus(OrderStatus.INVENTORY_RESERVED);
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.applyStatusUpdate(orderId, OrderStatus.CANCELLED))
                .isInstanceOf(IllegalTransitionException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        verify(orderRepository, never()).save(any(Order.class));
        verify(statusHistoryRepository, never()).save(any());
        verify(inventoryReleaseClient, never()).release(any(UUID.class));
    }
}
