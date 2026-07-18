package com.formalmethods.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * One append-only entry in an order's status history (spec.md's Key
 * Entities, FR-007). No setters beyond construction are exposed — history
 * entries are never modified or deleted (constitution Article III's
 * append-only intent for audit data).
 */
@Entity
@Table(name = "order_status_history")
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class StatusHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public StatusHistoryEntry(UUID orderId, OrderStatus status, Instant changedAt) {
        this.orderId = orderId;
        this.status = status;
        this.changedAt = changedAt;
    }
}
