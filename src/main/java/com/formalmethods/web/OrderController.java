package com.formalmethods.web;

import com.formalmethods.dto.HistoryEntryResponse;
import com.formalmethods.dto.OrderResponse;
import com.formalmethods.dto.StatusUpdateRequest;
import com.formalmethods.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for User Story 1 (plan.md's REST surface section):
 * create, apply a status update, and read current state — User Story 2:
 * cancel — and User Story 3: read status history.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder() {
        OrderResponse response = OrderResponse.from(orderService.create());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> applyStatusUpdate(
            @PathVariable UUID orderId, @Valid @RequestBody StatusUpdateRequest request) {
        OrderResponse response =
                OrderResponse.from(orderService.applyStatusUpdate(orderId, request.getTargetStatus()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        OrderResponse response = OrderResponse.from(orderService.getOrder(orderId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID orderId) {
        OrderResponse response = OrderResponse.from(orderService.cancel(orderId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<HistoryEntryResponse>> getHistory(@PathVariable UUID orderId) {
        List<HistoryEntryResponse> response = orderService.getHistory(orderId).stream()
                .map(HistoryEntryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
