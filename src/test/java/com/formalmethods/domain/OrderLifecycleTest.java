package com.formalmethods.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests (no Spring, no mocks) for the {@link OrderLifecycle}
 * transition table — spec.md's [US1] Formal Verification Obligation and
 * FR-002/003/004 exercised at the unit level. {@code OrderLifecycle} is
 * expected to expose a static legality relation, e.g.
 * {@code isLegalTransition(OrderStatus from, OrderStatus to)}, over the
 * {@link OrderStatus} enum (T005). This class does not exist yet (T004/T005
 * are pending), so these tests are expected to fail to compile until then.
 */
class OrderLifecycleTest {

    /**
     * [US1] / FR-002/003: the six forward edges NEW -> INVENTORY_RESERVED ->
     * PROVISIONED -> DISPATCHED -> DELIVERED -> CLOSED must each be admitted
     * as legal by the transition relation. Matters because these edges are
     * the entire "ordered lifecycle" the feature promises — if even one
     * forward edge were missing from the relation, no order could ever
     * legitimately reach later lifecycle stages.
     */
    @ParameterizedTest
    @MethodSource("legalForwardEdges")
    void forwardEdgeIsLegal(OrderStatus from, OrderStatus to) {
        assertThat(OrderLifecycle.isLegalTransition(from, to)).isTrue();
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
     * [US1] / FR-005: the three cancel edges from {NEW, INVENTORY_RESERVED,
     * PROVISIONED} to CANCELLED must each be admitted as legal. Matters
     * because cancellation strictly-before-DISPATCHED is an explicit,
     * safety-relevant part of the fixed lifecycle (US2) — if one of these
     * pre-DISPATCHED states could not legally cancel, a customer/operator
     * cancel would be wrongly rejected for that state.
     */
    @ParameterizedTest
    @MethodSource("legalCancelEdges")
    void cancelEdgeIsLegal(OrderStatus from) {
        assertThat(OrderLifecycle.isLegalTransition(from, OrderStatus.CANCELLED)).isTrue();
    }

    private static Stream<OrderStatus> legalCancelEdges() {
        return Stream.of(OrderStatus.NEW, OrderStatus.INVENTORY_RESERVED, OrderStatus.PROVISIONED);
    }

    /**
     * [US1]: the relation must admit EXACTLY the eight edges above (five
     * forward edges plus three cancel edges) and no others. Matters because
     * spec.md's [US1] obligation requires this to be established
     * exhaustively over all states and events, not sampled — a relation that
     * (for example) also legalized NEW->PROVISIONED directly would let an
     * order skip a lifecycle stage while still passing the individual
     * positive-edge tests above.
     */
    @Test
    void relationAdmitsExactlyTheEightLegalEdgesAndNoOthers() {
        Set<OrderStatus> allStates = EnumSet.allOf(OrderStatus.class);
        int legalEdgeCount = 0;
        for (OrderStatus from : allStates) {
            for (OrderStatus to : allStates) {
                if (OrderLifecycle.isLegalTransition(from, to)) {
                    legalEdgeCount++;
                }
            }
        }
        assertThat(legalEdgeCount).isEqualTo(8);
    }

    /**
     * [US1] / FR-004: illegal jumps that skip one or more lifecycle stages
     * (e.g. NEW->DISPATCHED, NEW->CLOSED) must never be admitted. Matters
     * because this is the exact failure mode spec.md's [US1] obligation
     * exists to rule out — an out-of-order transition being producible by
     * the transition function at all.
     */
    @ParameterizedTest
    @MethodSource("illegalSkipEdges")
    void skippingIntermediateStagesIsNeverLegal(OrderStatus from, OrderStatus to) {
        assertThat(OrderLifecycle.isLegalTransition(from, to)).isFalse();
    }

    private static Stream<Arguments> illegalSkipEdges() {
        return Stream.of(
                Arguments.of(OrderStatus.NEW, OrderStatus.DISPATCHED),
                Arguments.of(OrderStatus.NEW, OrderStatus.CLOSED),
                Arguments.of(OrderStatus.NEW, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.DISPATCHED),
                Arguments.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.CLOSED));
    }

    /**
     * [US1]: no cancel edge exists from DISPATCHED, DELIVERED, CLOSED, or
     * CANCELLED — cancellation is only legal strictly before DISPATCHED.
     * Matters because FR-005 requires a cancel at or after DISPATCHED to be
     * rejected; if the relation admitted a cancel edge from any of these
     * states, an in-flight or completed order could be wrongly cancelled.
     */
    @ParameterizedTest
    @MethodSource("statesWithNoLegalCancelEdge")
    void noCancelEdgeFromDispatchedOrLaterOrTerminalStates(OrderStatus from) {
        assertThat(OrderLifecycle.isLegalTransition(from, OrderStatus.CANCELLED)).isFalse();
    }

    private static Stream<OrderStatus> statesWithNoLegalCancelEdge() {
        return Stream.of(OrderStatus.DISPATCHED, OrderStatus.DELIVERED, OrderStatus.CLOSED, OrderStatus.CANCELLED);
    }

    /**
     * [US1] / FR-004: CLOSED has zero outgoing legal transitions of any
     * kind. Matters because CLOSED is one of the feature's two terminal
     * states — if any outgoing edge existed from it, "terminal" would not
     * actually hold, breaking the [US1] formal obligation's clause (b).
     */
    @Test
    void closedHasNoOutgoingLegalTransitions() {
        for (OrderStatus to : EnumSet.allOf(OrderStatus.class)) {
            assertThat(OrderLifecycle.isLegalTransition(OrderStatus.CLOSED, to)).isFalse();
        }
    }

    /**
     * [US1] / FR-004: CANCELLED has zero outgoing legal transitions of any
     * kind. Matters because CANCELLED is the feature's other terminal
     * state; testing only CLOSED (above) would leave half of clause (b) of
     * the [US1] obligation unverified.
     */
    @Test
    void cancelledHasNoOutgoingLegalTransitions() {
        for (OrderStatus to : EnumSet.allOf(OrderStatus.class)) {
            assertThat(OrderLifecycle.isLegalTransition(OrderStatus.CANCELLED, to)).isFalse();
        }
    }
}
