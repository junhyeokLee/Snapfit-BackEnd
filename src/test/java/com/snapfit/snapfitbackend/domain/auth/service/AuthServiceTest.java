package com.snapfit.snapfitbackend.domain.auth.service;

import com.snapfit.snapfitbackend.domain.auth.dto.AuthResponse;
import com.snapfit.snapfitbackend.domain.auth.entity.RefreshToken;
import com.snapfit.snapfitbackend.domain.auth.entity.UserEntity;
import com.snapfit.snapfitbackend.domain.auth.repository.RefreshTokenRepository;
import com.snapfit.snapfitbackend.domain.auth.repository.UserRepository;
import com.snapfit.snapfitbackend.domain.image.ImageStorageService;
import com.snapfit.snapfitbackend.network.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuthService authService;

    @Test
    void refresh_issuesNewTokens_whenValidRefreshToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .userId(10L)
                .token("refresh-old")
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();
        UserEntity user = UserEntity.builder()
                .id(10L)
                .name("Tester")
                .provider("KAKAO")
                .build();

        when(jwtProvider.validateToken("refresh-old")).thenReturn(true);
        when(refreshTokenRepository.findByToken("refresh-old"))
                .thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken("10")).thenReturn("access-new");
        when(jwtProvider.createRefreshToken("10")).thenReturn("refresh-new");
        when(jwtProvider.getRefreshTokenExpiration()).thenReturn(1000L);

        AuthResponse response = authService.refresh("refresh-old");

        assertEquals("access-new", response.getAccessToken());
        assertEquals("refresh-new", response.getRefreshToken());
        assertEquals(10L, response.getUser().getId());

        verify(refreshTokenRepository).delete(refreshToken);
        verify(refreshTokenRepository).deleteByUserId(10L);
        verify(refreshTokenRepository).flush();
        verify(refreshTokenRepository).save(org.mockito.ArgumentMatchers.any(RefreshToken.class));
    }

    @Test
    void refresh_throwsWhenInvalidToken() {
        when(jwtProvider.validateToken("bad-token")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.refresh("bad-token"));
    }
}
