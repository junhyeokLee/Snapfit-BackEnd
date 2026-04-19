package com.snapfit.snapfitbackend.domain.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StorageQuotaResponse {
    private String userId;
    private String planCode;
    private long usedBytes;
    private long softLimitBytes;
    private long hardLimitBytes;
    private boolean softExceeded;
    private boolean hardExceeded;
    private int usagePercent;
    private LocalDateTime measuredAt;
}
