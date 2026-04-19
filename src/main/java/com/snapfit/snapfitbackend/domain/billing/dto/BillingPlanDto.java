package com.snapfit.snapfitbackend.domain.billing.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillingPlanDto {
    private String planCode;
    private String title;
    private int amount;
    private String currency;
    private int periodDays;
    private String provider;
}
