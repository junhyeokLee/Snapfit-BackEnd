package com.snapfit.snapfitbackend.domain.album.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 페이지 조회/저장 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class AlbumPageResponse {

    private Long pageId;
    private Long albumId;
    private int pageNumber;
    private String layersJson;
    private String imageUrl;
    private String thumbnailUrl;
}