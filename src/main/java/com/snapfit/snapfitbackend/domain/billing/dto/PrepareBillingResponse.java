package com.snapfit.snapfitbackend.domain.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PrepareBillingResponse {
    private String orderId;
    private String planCode;
    private String provider;
    private int amount;
    private String currency;
    private String checkoutUrl;
    private String successUrl;
    private String failUrl;
    private LocalDateTime expiresAt;
    @com.fasterxml.jackson.annotation.JsonProperty("isMock")
    private boolean isMock;
}
