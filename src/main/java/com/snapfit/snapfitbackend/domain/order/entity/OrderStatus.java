package com.snapfit.snapfitbackend.domain.order.entity;

public enum OrderStatus {
    PAYMENT_PENDING("결제대기", 0.18),
    PAYMENT_COMPLETED("결제완료", 0.32),
    IN_PRODUCTION("제작중", 0.58),
    SHIPPING("배송중", 0.82),
    DELIVERED("배송완료", 1.0),
    CANCELED("취소", 0.10);

    private final String label;
    private final double progress;

    OrderStatus(String label, double progress) {
        this.label = label;
        this.progress = progress;
    }

    public String getLabel() {
        return label;
    }

    public double getProgress() {
        return progress;
    }

    public OrderStatus next() {
        return switch (this) {
            case PAYMENT_PENDING -> PAYMENT_COMPLETED;
            case PAYMENT_COMPLETED -> IN_PRODUCTION;
            case IN_PRODUCTION -> SHIPPING;
            case SHIPPING -> DELIVERED;
            case DELIVERED, CANCELED -> this;
        };
    }
}
