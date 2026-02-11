package com.snapfit.snapfitbackend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleUserDto {
    private String sub; // Google User ID
    private String email;
    private String name;
    private String picture; // Profile Image URL
}
