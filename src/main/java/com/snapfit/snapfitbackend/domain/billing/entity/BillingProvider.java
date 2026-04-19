package com.snapfit.snapfitbackend.domain.billing.entity;

import java.util.Locale;

public enum BillingProvider {
    TOSS_NAVERPAY,
    INICIS_NAVERPAY;

    public static BillingProvider from(String raw) {
        if (raw == null || raw.isBlank()) {
            return TOSS_NAVERPAY;
        }
        try {
            return BillingProvider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return TOSS_NAVERPAY;
        }
    }
}
