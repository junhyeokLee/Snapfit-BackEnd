package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.auth.dto.AuthResponse;
import com.snapfit.snapfitbackend.domain.auth.dto.GoogleLoginRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.KakaoLoginRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.RefreshRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.UserInfo;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login/kakao")
    public AuthResponse loginWithKakao(@RequestBody KakaoLoginRequest request) {
        return buildMockResponse("KAKAO", request.getAccessToken());
    }

    @PostMapping("/login/google")
    public AuthResponse loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        return buildMockResponse("GOOGLE", request.getIdToken());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        return buildMockResponse("REFRESH", request.getRefreshToken());
    }

    private AuthResponse buildMockResponse(String provider, String rawToken) {
        final String accessToken = UUID.randomUUID().toString();
        final String refreshToken = UUID.randomUUID().toString();
        final int expiresIn = 3600;

        final long userId = Math.abs((rawToken == null ? 0 : rawToken.hashCode()));
        final UserInfo user = UserInfo.builder()
                .id(userId)
                .email(null)
                .name(provider + "_USER")
                .profileImageUrl(null)
                .provider(provider)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
