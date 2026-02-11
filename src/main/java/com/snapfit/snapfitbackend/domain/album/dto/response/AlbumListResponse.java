package com.snapfit.snapfitbackend.domain.album.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 앨범 목록 조회용 DTO (내가 저장한 앨범만)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumListResponse {

    private Long albumId;
    private String ratio;
    private String coverThumbnailUrl;
    private String coverImageUrl;
    private Integer totalPages;
    private Integer targetPages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
