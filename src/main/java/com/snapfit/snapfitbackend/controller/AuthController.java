package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.auth.dto.AuthResponse;
import com.snapfit.snapfitbackend.domain.auth.dto.GoogleLoginRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.KakaoLoginRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.RefreshRequest;
import com.snapfit.snapfitbackend.domain.auth.dto.UserInfo;
import com.snapfit.snapfitbackend.domain.auth.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/kakao")
    public AuthResponse loginWithKakao(@RequestBody KakaoLoginRequest request) {
        return authService.loginWithKakao(request.getAccessToken());
    }

    @PostMapping("/login/google")
    public AuthResponse loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request.getIdToken());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        try {
            AuthResponse response = authService.refresh(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * 프로필 수정 (이미지 파일 업로드 지원)
     * Authorization 헤더 필수
     */
    @RequestMapping(value = "/profile", method = { RequestMethod.PATCH,
            RequestMethod.POST }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestPart(value = "name", required = false) String name,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {

        try {
            UserInfo userInfo = authService.updateProfile(authorization, name, profileImage);
            return ResponseEntity.ok(userInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            log.error("Profile update failed. namePresent={}, imagePresent={}, imageSize={}",
                    name != null && !name.isBlank(),
                    profileImage != null && !profileImage.isEmpty(),
                    profileImage != null ? profileImage.getSize() : 0L,
                    e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "PROFILE_UPLOAD_FAILED",
                    "message", e.getMessage() == null ? "internal error" : e.getMessage()));
        }
    }
}
