package com.snapfit.snapfitbackend.domain.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StoragePreflightResponse {
    private String userId;
    private String planCode;
    private long incomingBytes;
    private long usedBytes;
    private long projectedBytes;
    private long hardLimitBytes;
    private long remainingBytes;
    private boolean allowed;
    private String reason;
    private LocalDateTime measuredAt;
}
