package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.auth.dto.AuthResponse;
import com.snapfit.snapfitbackend.domain.auth.dto.GoogleLoginRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.KakaoLoginRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.RefreshRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.UpdateProfileRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.UserInfo;
import com.snapfit.snapfitbackend.domain.auth.entity.UserEntity;
import com.snapfit.snapfitbackend.domain.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 토큰별 유저 정보 저장 (프로필 수정 시 갱신) */
    private static final Map<String, UserInfo> TOKEN_TO_USER = new ConcurrentHashMap<>();
    /** 리프레시 토큰 → 유저 (리프레시 시 동일 유저 반환용) */
    private static final Map<String, UserInfo> REFRESH_TO_USER = new ConcurrentHashMap<>();

    @PostMapping("/login/kakao")
    public AuthResponse loginWithKakao(@RequestBody KakaoLoginRequest request) {
        AuthResponse response = buildMockResponse("KAKAO", request.getAccessToken(), null);
        persistUserIfNew(response.getUser());
        TOKEN_TO_USER.put(response.getAccessToken(), response.getUser());
        TOKEN_TO_USER.put(response.getRefreshToken(), response.getUser());
        REFRESH_TO_USER.put(response.getRefreshToken(), response.getUser());
        return response;
    }

    @PostMapping("/login/google")
    public AuthResponse loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        AuthResponse response = buildMockResponse("GOOGLE", request.getIdToken(), null);
        persistUserIfNew(response.getUser());
        TOKEN_TO_USER.put(response.getAccessToken(), response.getUser());
        TOKEN_TO_USER.put(response.getRefreshToken(), response.getUser());
        REFRESH_TO_USER.put(response.getRefreshToken(), response.getUser());
        return response;
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        UserInfo existingUser = REFRESH_TO_USER.get(request.getRefreshToken());
        AuthResponse response = buildMockResponse("REFRESH", request.getRefreshToken(), existingUser);
        TOKEN_TO_USER.put(response.getAccessToken(), response.getUser());
        REFRESH_TO_USER.put(response.getRefreshToken(), response.getUser());
        return response;
    }

    /**
     * 프로필 수정 (프로필 이미지 URL, 이름) — DB에 영구 저장
     * Authorization: Bearer {accessToken} 필요
     */
    @RequestMapping(value = "/profile", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<UserInfo> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody UpdateProfileRequest body) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        UserInfo existing = TOKEN_TO_USER.get(token);
        if (existing == null) {
            return ResponseEntity.status(401).build();
        }
        String newName = body.getName() != null && !body.getName().isBlank() ? body.getName() : existing.getName();
        String newProfileUrl = body.getProfileImageUrl() != null && !body.getProfileImageUrl().isBlank()
                ? body.getProfileImageUrl()
                : existing.getProfileImageUrl();

        UserInfo updated = UserInfo.builder()
                .id(existing.getId())
                .email(existing.getEmail())
                .name(newName)
                .profileImageUrl(newProfileUrl)
                .provider(existing.getProvider())
                .build();

        userRepository.findById(existing.getId()).ifPresent(entity -> {
            entity.setName(newName);
            entity.setProfileImageUrl(newProfileUrl);
            userRepository.save(entity);
        });

        TOKEN_TO_USER.replaceAll((k, v) -> v.getId().equals(updated.getId()) ? updated : v);
        TOKEN_TO_USER.put(token, updated);
        REFRESH_TO_USER.replaceAll((k, v) -> v.getId().equals(updated.getId()) ? updated : v);
        return ResponseEntity.ok(updated);
    }

    /** 신규 유저면 DB에 저장, 기존 유저면 DB에서 프로필(이미지 URL 등) 로드해 반영 */
    private void persistUserIfNew(UserInfo user) {
        long userId = user.getId();
        UserEntity entity = userRepository.findById(userId).orElse(null);
        if (entity == null) {
            userRepository.save(UserEntity.builder()
                    .id(userId)
                    .email(user.getEmail())
                    .name(user.getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .provider(user.getProvider())
                    .build());
        } else {
            user.setName(entity.getName() != null ? entity.getName() : user.getName());
            user.setProfileImageUrl(entity.getProfileImageUrl() != null ? entity.getProfileImageUrl() : user.getProfileImageUrl());
            user.setEmail(entity.getEmail() != null ? entity.getEmail() : user.getEmail());
        }
    }

    private AuthResponse buildMockResponse(String provider, String rawToken, UserInfo existingUser) {
        final String accessToken = UUID.randomUUID().toString();
        final String refreshToken = UUID.randomUUID().toString();
        final int expiresIn = 3600;

        final long userId = Math.abs((rawToken == null ? 0 : rawToken.hashCode()));
        UserInfo user;
        if (existingUser != null) {
            user = existingUser;
        } else {
            UserEntity fromDb = userRepository.findById(userId).orElse(null);
            if (fromDb != null) {
                user = UserInfo.builder()
                        .id(fromDb.getId())
                        .email(fromDb.getEmail())
                        .name(fromDb.getName())
                        .profileImageUrl(fromDb.getProfileImageUrl())
                        .provider(fromDb.getProvider() != null ? fromDb.getProvider() : provider)
                        .build();
            } else {
                user = UserInfo.builder()
                        .id(userId)
                        .email(null)
                        .name(provider + "_USER")
                        .profileImageUrl(null)
                        .provider(provider)
                        .build();
            }
        }

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
