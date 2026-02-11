package com.snapfit.snapfitbackend.domain.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor // JSON 변환을 위해 필수
@AllArgsConstructor
public class UpdateProfileRequest {
    private String profileImageUrl;
    private String name;
}