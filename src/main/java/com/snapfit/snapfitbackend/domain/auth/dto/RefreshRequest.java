package com.snapfit.snapfitbackend.domain.auth.dto;

import lombok.Getter;

@Getter
public class RefreshRequest {
    private String refreshToken;
}
