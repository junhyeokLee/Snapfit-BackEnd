package com.snapfit.snapfitbackend.domain.album.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CreateAlbumResponse {
    @JsonProperty("albumId") // Flutter의 albumId와 매핑
    private Long albumId;

    @JsonProperty("ratio") // Flutter의 coverRatio와 매핑
    private String ratio;

    private Integer targetPages;
    private String coverLayersJson;
    private String coverTheme;
    private String coverOriginalUrl;
    private String coverPreviewUrl;
    private String coverImageUrl;
    private String coverThumbnailUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") // 날짜 형식 고정
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
