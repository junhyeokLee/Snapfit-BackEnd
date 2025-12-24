package com.snapfit.snapfitbackend.domain.album.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 앨범 생성 요청 DTO
 * - 플러터에서 앨범 생성할 때 보내는 기본 데이터
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateAlbumRequest {

    // 앨범 / 커버 비율 (예: "3:4", "1:1", "A4")
    private String ratio;

    // Flutter LayerModel[] 직렬화 JSON (커버 레이어 전체)
    private String coverLayersJson;

    // 커버 고해상도 렌더 이미지 URL
    private String coverImageUrl;

    // 커버 썸네일 이미지 URL
    private String coverThumbnailUrl;

}