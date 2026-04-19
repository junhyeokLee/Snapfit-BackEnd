package com.snapfit.snapfitbackend.domain.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionStatusResponse {
    private String userId;
    private String planCode;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime nextBillingAt;
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private boolean isActive;
}
