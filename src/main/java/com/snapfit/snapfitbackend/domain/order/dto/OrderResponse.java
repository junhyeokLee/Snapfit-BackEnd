package com.snapfit.snapfitbackend.domain.order.dto;

import com.snapfit.snapfitbackend.domain.order.entity.OrderEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderResponse {
    private String orderId;
    private String userId;
    private String title;
    private int amount;
    private String status;
    private String statusLabel;
    private double progress;
    private LocalDateTime orderedAt;
    private Long albumId;
    private Integer pageCount;
    private String recipientName;
    private String recipientPhone;
    private String zipCode;
    private String addressLine1;
    private String addressLine2;
    private String deliveryMemo;
    private String paymentMethod;
    private String courier;
    private String trackingNumber;
    private String printVendor;
    private String printVendorOrderId;
    private String printPackageJsonUrl;
    private String printFilePdfUrl;
    private String printFileZipUrl;
    private Integer printAssetCount;
    private LocalDateTime paymentConfirmedAt;
    private LocalDateTime printPackageGeneratedAt;
    private LocalDateTime printSubmittedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    public static OrderResponse from(OrderEntity entity) {
        return OrderResponse.builder()
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .amount(entity.getAmount())
                .status(entity.getStatus().name())
                .statusLabel(entity.getStatus().getLabel())
                .progress(entity.getStatus().getProgress())
                .orderedAt(entity.getCreatedAt())
                .albumId(entity.getAlbumId())
                .pageCount(entity.getPageCount())
                .recipientName(entity.getRecipientName())
                .recipientPhone(entity.getRecipientPhone())
                .zipCode(entity.getZipCode())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .deliveryMemo(entity.getDeliveryMemo())
                .paymentMethod(entity.getPaymentMethod())
                .courier(entity.getCourier())
                .trackingNumber(entity.getTrackingNumber())
                .printVendor(entity.getPrintVendor())
                .printVendorOrderId(entity.getPrintVendorOrderId())
                .printPackageJsonUrl(entity.getPrintPackageJsonUrl())
                .printFilePdfUrl(entity.getPrintFilePdfUrl())
                .printFileZipUrl(entity.getPrintFileZipUrl())
                .printAssetCount(entity.getPrintAssetCount())
                .paymentConfirmedAt(entity.getPaymentConfirmedAt())
                .printPackageGeneratedAt(entity.getPrintPackageGeneratedAt())
                .printSubmittedAt(entity.getPrintSubmittedAt())
                .shippedAt(entity.getShippedAt())
                .deliveredAt(entity.getDeliveredAt())
                .build();
    }
}
