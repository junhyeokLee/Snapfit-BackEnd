package com.snapfit.snapfitbackend.domain.template.dto.response;

import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TemplateResponse {
    private Long id;
    private String title;
    private String subTitle;
    private String description;
    private String coverImageUrl;
    private List<String> previewImages;
    private int pageCount;
    private int likeCount;
    private int userCount;
    private String category;
    private List<String> tags;
    private int weeklyScore;
    private LocalDateTime newUntil;
    @com.fasterxml.jackson.annotation.JsonProperty("isBest")
    private boolean isBest;
    @com.fasterxml.jackson.annotation.JsonProperty("isPremium")
    private boolean isPremium;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @com.fasterxml.jackson.annotation.JsonProperty("isLiked")
    private boolean isLiked;
    @com.fasterxml.jackson.annotation.JsonProperty("isNew")
    private boolean isNew;

    // Use raw string for flexibility in frontend parsing
    private String templateJson;

    public static TemplateResponse from(
            TemplateEntity entity,
            List<String> previewImages,
            List<String> tags,
            boolean isLiked,
            boolean isNew) {
        return TemplateResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .subTitle(entity.getSubTitle())
                .description(entity.getDescription())
                .coverImageUrl(entity.getCoverImageUrl())
                .previewImages(previewImages)
                .pageCount(entity.getPageCount())
                .likeCount(entity.getLikeCount())
                .userCount(entity.getUserCount())
                .category(entity.getCategory())
                .tags(tags)
                .weeklyScore(entity.getWeeklyScore() == null ? 0 : entity.getWeeklyScore())
                .newUntil(entity.getNewUntil())
                .isBest(entity.isBest())
                .isPremium(entity.isPremium())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isLiked(isLiked)
                .isNew(isNew)
                .templateJson(entity.getTemplateJson())
                .build();
    }
}
