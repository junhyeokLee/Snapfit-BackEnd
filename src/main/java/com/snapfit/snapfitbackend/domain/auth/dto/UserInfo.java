package com.snapfit.snapfitbackend.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfo {
    private Long id;
    private String email;
    private String name;
    private String profileImageUrl;
    private String provider;
}
