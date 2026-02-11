package com.snapfit.snapfitbackend.domain.album.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InviteInfoResponse {
    private Long albumId;
    private String ratio;
    private String coverThumbnailUrl;
    private String coverImageUrl;
    private String status;
}
