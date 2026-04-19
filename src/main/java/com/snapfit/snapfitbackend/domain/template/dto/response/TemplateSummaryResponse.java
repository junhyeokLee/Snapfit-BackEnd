package com.snapfit.snapfitbackend.domain.template.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TemplateSummaryResponse {
    private Long id;
    private String title;
    private String coverImageUrl;
    private List<String> tags;
    private int weeklyScore;
    private int likeCount;
    private int userCount;

    @com.fasterxml.jackson.annotation.JsonProperty("isPremium")
    private boolean isPremium;

    @com.fasterxml.jackson.annotation.JsonProperty("isBest")
    private boolean isBest;

    @com.fasterxml.jackson.annotation.JsonProperty("isNew")
    private boolean isNew;

    @com.fasterxml.jackson.annotation.JsonProperty("isLiked")
    private boolean isLiked;

    public static TemplateSummaryResponse of(
            Long id,
            String title,
            String coverImageUrl,
            List<String> tags,
            int weeklyScore,
            int likeCount,
            int userCount,
            boolean isPremium,
            boolean isBest,
            boolean isNew,
            boolean isLiked) {
        return TemplateSummaryResponse.builder()
                .id(id)
                .title(title)
                .coverImageUrl(coverImageUrl)
                .tags(tags)
                .weeklyScore(weeklyScore)
                .likeCount(likeCount)
                .userCount(userCount)
                .isPremium(isPremium)
                .isBest(isBest)
                .isNew(isNew)
                .isLiked(isLiked)
                .build();
    }
}
