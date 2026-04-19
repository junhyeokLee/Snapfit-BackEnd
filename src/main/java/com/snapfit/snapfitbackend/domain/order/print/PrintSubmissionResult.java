package com.snapfit.snapfitbackend.domain.order.print;

public record PrintSubmissionResult(
        boolean accepted,
        String vendor,
        String vendorOrderId,
        String message) {
}

