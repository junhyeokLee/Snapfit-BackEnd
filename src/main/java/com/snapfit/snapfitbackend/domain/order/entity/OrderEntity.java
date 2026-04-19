package com.snapfit.snapfitbackend.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_order_id", columnNames = { "order_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false)
    private int amount;

    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "recipient_name", length = 120)
    private String recipientName;

    @Column(name = "recipient_phone", length = 40)
    private String recipientPhone;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "delivery_memo", length = 255)
    private String deliveryMemo;

    @Column(name = "payment_method", length = 40)
    private String paymentMethod;

    @Column(name = "payment_confirmed_at")
    private LocalDateTime paymentConfirmedAt;

    @Column(name = "print_vendor", length = 40)
    private String printVendor;

    @Column(name = "print_vendor_order_id", length = 120)
    private String printVendorOrderId;

    @Column(name = "print_package_json_url", length = 1000)
    private String printPackageJsonUrl;

    @Column(name = "print_file_pdf_url", length = 1000)
    private String printFilePdfUrl;

    @Column(name = "print_file_zip_url", length = 1000)
    private String printFileZipUrl;

    @Column(name = "print_asset_count")
    private Integer printAssetCount;

    @Column(name = "print_package_generated_at")
    private LocalDateTime printPackageGeneratedAt;

    @Column(name = "print_submitted_at")
    private LocalDateTime printSubmittedAt;

    @Column(name = "courier", length = 60)
    private String courier;

    @Column(name = "tracking_number", length = 120)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false)
    private double progress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = OrderStatus.PAYMENT_PENDING;
        }
        progress = status.getProgress();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        progress = status.getProgress();
    }
}
