package com.formalmethods.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formalmethods.domain.Order;
import com.formalmethods.domain.OrderStatus;
import com.formalmethods.dto.StatusUpdateRequest;
import com.formalmethods.service.IllegalTransitionException;
import com.formalmethods.service.OrderNotFoundException;
import com.formalmethods.service.OrderService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller-slice tests (T013, US1) for {@code POST /api/orders},
 * {@code POST /api/orders/{orderId}/status}, and
 * {@code GET /api/orders/{orderId}}. Uses {@code @WebMvcTest} with a
 * Mockito-mocked {@link OrderService} — only the web layer boots, no full
 * Spring context, no real/embedded database (constitution Article IV).
 * References {@code OrderController} and related DTOs/exceptions that do
 * not exist yet; expected to fail to compile until T014/T015/T016 land.
 */
@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private static Order orderWithStatus(UUID id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setInventoryReserved(status == OrderStatus.INVENTORY_RESERVED
                || status == OrderStatus.PROVISIONED);
        return order;
    }

    /**
     * US1 Acceptance Scenario (create) / FR-001: {@code POST /api/orders}
     * returns 201 with the created order, which starts in NEW. Matters
     * because create is the entry point for the entire lifecycle — if it
     * didn't return 201 with a NEW-state order, no client could reliably
     * begin driving an order through the state machine.
     */
    @Test
    void createOrderReturns201WithNewOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.create()).thenReturn(orderWithStatus(orderId, OrderStatus.NEW));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    /**
     * US1 Acceptance Scenario 1 / FR-003: {@code POST
     * /api/orders/{orderId}/status} with a legal target returns 200 with the
     * updated order. Matters because this is the externally observable half
     * of "one legal step at a time" — a correct service-layer transition
     * that the controller failed to surface as a 200 would be
     * indistinguishable from a broken feature to any caller.
     */
    @Test
    void applyLegalStatusUpdateReturns200WithUpdatedOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.applyStatusUpdate(eq(orderId), eq(OrderStatus.INVENTORY_RESERVED)))
                .thenReturn(orderWithStatus(orderId, OrderStatus.INVENTORY_RESERVED));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setTargetStatus(OrderStatus.INVENTORY_RESERVED);

        mockMvc.perform(post("/api/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVENTORY_RESERVED"));
    }

    /**
     * US1 Acceptance Scenario 2 / FR-003: an illegal transition rejected by
     * the service surfaces as plan.md's designated rejection status (409)
     * rather than a 200 or an unmapped 500. Matters because a client
     * relying on the HTTP status to know whether its update took effect
     * would otherwise be unable to distinguish an illegal-jump rejection
     * from success or from an unrelated server error.
     */
    @Test
    void applyIllegalStatusUpdateReturns409() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.applyStatusUpdate(eq(orderId), any(OrderStatus.class)))
                .thenThrow(new IllegalTransitionException("illegal transition"));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setTargetStatus(OrderStatus.DISPATCHED);

        mockMvc.perform(post("/api/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    /**
     * FR-015 / Edge Case ("references an order that does not exist"): a
     * status update against a non-existent order returns 404 per plan.md's
     * HTTP code mapping. Matters because FR-015 requires this rejected
     * outcome without creating any order or history entry — a 200 or 500
     * here would either silently fabricate state or leak an unmapped
     * server error to the caller.
     */
    @Test
    void applyStatusUpdateOnUnknownOrderReturns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.applyStatusUpdate(eq(orderId), any(OrderStatus.class)))
                .thenThrow(new OrderNotFoundException("order not found"));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setTargetStatus(OrderStatus.INVENTORY_RESERVED);

        mockMvc.perform(post("/api/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    /**
     * US1 Acceptance Scenario 5 / FR-009: {@code GET /api/orders/{orderId}}
     * returns 200 with the order's current lifecycle state. Matters because
     * read-state is the only externally observable way to confirm any of
     * US1's other scenarios actually happened.
     */
    @Test
    void getOrderReturns200WithCurrentState() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrder(orderId)).thenReturn(orderWithStatus(orderId, OrderStatus.PROVISIONED));

        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVISIONED"));
    }

    /**
     * FR-015 / Edge Case: {@code GET /api/orders/{orderId}} for a
     * non-existent order returns 404. Matters because read-state must fail
     * closed on an unknown id rather than returning a fabricated or empty
     * 200 body that a caller could mistake for a real order.
     */
    @Test
    void getOrderForUnknownOrderReturns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrder(orderId)).thenThrow(new OrderNotFoundException("order not found"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }

    /**
     * US2 Acceptance Scenario 1/2 / FR-005/FR-006: {@code POST
     * /api/orders/{orderId}/cancel} against an order the service accepts
     * for cancellation returns 200 with the now-CANCELLED order, per
     * plan.md's REST surface ("200 OK when cancelled ... or an idempotent
     * repeat"). Matters because this is the only externally observable
     * confirmation that a cancel (with or without an inventory release)
     * actually took effect.
     */
    @Test
    void cancelOrderReturns200WithCancelledOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.cancel(orderId)).thenReturn(orderWithStatus(orderId, OrderStatus.CANCELLED));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    /**
     * US2 Acceptance Scenario 4 / FR-011: repeating a cancel against an
     * order already CANCELLED still returns 200 (idempotent no-op), not an
     * error. Matters because a client re-delivering a cancel request (at-
     * least-once semantics, FR-011) must see success both times, not a
     * spurious failure on the second call.
     */
    @Test
    void repeatedCancelOnAlreadyCancelledOrderReturns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.cancel(orderId)).thenReturn(orderWithStatus(orderId, OrderStatus.CANCELLED));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    /**
     * US2 Acceptance Scenario 3 / FR-005: {@code POST
     * /api/orders/{orderId}/cancel} against an order the service rejects
     * (e.g. already DISPATCHED) returns 409 per plan.md's exact HTTP
     * mapping ("409 cancel at/after DISPATCHED or on CLOSED"). Matters
     * because a client must be able to distinguish "cancel was too late" from
     * success (200) or an unrelated server error (500).
     */
    @Test
    void cancelOrderAtOrAfterDispatchedReturns409() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.cancel(orderId)).thenThrow(new IllegalTransitionException("cancel rejected"));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}
