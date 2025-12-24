package com.snapfit.snapfitbackend.domain.album.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 앨범 생성 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class CreateAlbumResponse {

    private Long albumId;
    private String ratio;
    private String coverLayersJson;
    private String coverImageUrl;
    private String coverThumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}