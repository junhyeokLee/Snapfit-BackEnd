package com.snapfit.snapfitbackend.domain.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_order", indexes = {
        @Index(name = "idx_billing_order_user", columnList = "user_id"),
        @Index(name = "idx_billing_order_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_billing_order_order_id", columnNames = { "order_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "plan_code", nullable = false, length = 40)
    private String planCode;

    @Column(nullable = false, length = 32)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingOrderStatus status;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "reserve_id", length = 120)
    private String reserveId;

    @Column(name = "transaction_id", length = 120)
    private String transactionId;

    @Column(name = "fail_reason", length = 600)
    private String failReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
