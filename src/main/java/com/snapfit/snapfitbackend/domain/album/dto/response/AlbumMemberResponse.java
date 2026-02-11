package com.snapfit.snapfitbackend.domain.album.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlbumMemberResponse {
    private Long id;
    private Long albumId;
    private String userId;
    private String role;
    private String status;
}
