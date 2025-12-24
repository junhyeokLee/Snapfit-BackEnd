// src/main/java/com/snapfit/snapfitbackend/domain/album/dto/response/AlbumDetailResponse.java
package com.snapfit.snapfitbackend.domain.album.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDetailResponse {

    // 앨범 기본 정보
    private Long albumId;
    private String ratio;

    private String coverLayersJson;
    private String coverImageUrl;
    private String albumThumbnailUrl;

    private Integer totalPages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 페이지 목록
    private List<AlbumPageSummaryResponse> pages;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlbumPageSummaryResponse {
        private Long pageId;
        private int pageNumber;
        private String imageUrl;
        private String thumbnailUrl;
    }
}