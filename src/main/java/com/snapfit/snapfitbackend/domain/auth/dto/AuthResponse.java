package com.snapfit.snapfitbackend.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private int expiresIn;
    private UserInfo user;
}
