package com.snapfit.snapfitbackend.domain.album.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 페이지 저장 요청 DTO
 * - 특정 albumId에 대해 pageNumber, layersJson 을 저장
 */
@Getter
@Setter
@NoArgsConstructor
public class SaveAlbumPageRequest {

    // 어느 앨범에 붙일 페이지인지
    private Long albumId;

    // 페이지 번호 (없으면 마지막 + 1로 자동 부여하는 정책도 가능)
    private Integer pageNumber;

    private String layersJson;

    // 페이지 원본(프린트용) URL
    private String originalUrl;

    // 페이지 미리보기(앱용) URL
    private String previewUrl;

    private String imageUrl;

    private String thumbnailUrl;
}