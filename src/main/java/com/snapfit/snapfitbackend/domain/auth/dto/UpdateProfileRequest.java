package com.snapfit.snapfitbackend.domain.auth.dto;

import lombok.Getter;

@Getter
public class UpdateProfileRequest {
    private String profileImageUrl;
    private String name;
}
