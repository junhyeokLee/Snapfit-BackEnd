package com.snapfit.snapfitbackend.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsentUpdateRequest {
    private String termsVersion;
    private String privacyVersion;
    private Boolean marketingOptIn;
    private String agreedAt;
}

