package com.snapfit.snapfitbackend.domain.album.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InviteLinkResponse {
    private Long albumId;
    private String token;
    private String link;
}
