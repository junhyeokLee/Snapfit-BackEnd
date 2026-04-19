package com.snapfit.snapfitbackend.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserNotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String body;
    private String deeplink;
    private String targetTopic;
    private LocalDateTime createdAt;
    @com.fasterxml.jackson.annotation.JsonProperty("isRead")
    private boolean isRead;
}
