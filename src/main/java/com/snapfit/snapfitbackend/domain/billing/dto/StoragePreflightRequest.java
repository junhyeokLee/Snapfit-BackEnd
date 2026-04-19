package com.snapfit.snapfitbackend.domain.billing.dto;

import lombok.Data;

@Data
public class StoragePreflightRequest {
    private String userId;
    private long incomingBytes;
}
