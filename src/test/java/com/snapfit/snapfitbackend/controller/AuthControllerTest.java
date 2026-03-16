package com.snapfit.snapfitbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.auth.dto.AuthResponse;
import com.snapfit.snapfitbackend.domain.auth.dto.UserInfo;
import com.snapfit.snapfitbackend.domain.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void loginWithKakao_returnsAuthResponse() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .expiresIn(3600)
                .user(UserInfo.builder()
                        .id(1L)
                        .name("Tester")
                        .provider("KAKAO")
                        .build())
                .build();

        when(authService.loginWithKakao("kakao-token")).thenReturn(response);

        mockMvc.perform(post("/api/auth/login/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("accessToken", "kakao-token")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.provider").value("KAKAO"));
    }

    @Test
    void loginWithGoogle_returnsAuthResponse() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-g")
                .refreshToken("refresh-g")
                .expiresIn(3600)
                .user(UserInfo.builder()
                        .id(2L)
                        .name("Tester G")
                        .provider("GOOGLE")
                        .build())
                .build();

        when(authService.loginWithGoogle("google-token")).thenReturn(response);

        mockMvc.perform(post("/api/auth/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("idToken", "google-token")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-g"))
                .andExpect(jsonPath("$.user.provider").value("GOOGLE"));
    }

    @Test
    void refresh_returnsUnauthorizedOnInvalidToken() throws Exception {
        when(authService.refresh("bad-token"))
                .thenThrow(new IllegalArgumentException("invalid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", "bad-token")
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_returnsUserInfo() throws Exception {
        UserInfo userInfo = UserInfo.builder()
                .id(10L)
                .name("Profile")
                .provider("KAKAO")
                .build();

        when(authService.updateProfile(anyString(), anyString(), any()))
                .thenReturn(userInfo);

        MockMultipartFile namePart = new MockMultipartFile(
                "name",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "Profile".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/auth/profile")
                        .file(namePart)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Profile"));
    }

    @Test
    void updateProfile_returnsUnauthorizedOnInvalidToken() throws Exception {
        doThrow(new IllegalArgumentException("invalid token"))
                .when(authService).updateProfile(anyString(), anyString(), any());

        MockMultipartFile namePart = new MockMultipartFile(
                "name",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "Profile".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/auth/profile")
                        .file(namePart)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isUnauthorized());
    }
}
