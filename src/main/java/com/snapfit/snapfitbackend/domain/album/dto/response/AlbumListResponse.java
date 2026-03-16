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
    private String title; // 앨범 제목
    private String coverLayersJson; // 커버 레이어 JSON
    private String coverTheme; // 커버 테마
    private String coverThumbnailUrl;
    private String coverImageUrl;
    private Integer totalPages;
    private Integer targetPages;
    private String lockedBy; // 현재 편집 중인 사용자 이름 (없으면 null)
    private String lockedById; // 현재 편집 중인 사용자 ID (없으면 null)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
