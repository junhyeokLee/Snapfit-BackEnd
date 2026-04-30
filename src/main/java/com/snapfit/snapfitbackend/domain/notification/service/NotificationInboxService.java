package com.snapfit.snapfitbackend.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.notification.dto.UserNotificationResponse;
import com.snapfit.snapfitbackend.domain.notification.entity.NotificationInboxEntity;
import com.snapfit.snapfitbackend.domain.notification.entity.NotificationReadEntity;
import com.snapfit.snapfitbackend.domain.notification.repository.NotificationInboxRepository;
import com.snapfit.snapfitbackend.domain.notification.repository.NotificationReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationInboxService {

    private final NotificationInboxRepository notificationInboxRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final ObjectMapper objectMapper;
    @Value("${snapfit.notifications.retention-days:90}")
    private int retentionDays;

    @Transactional
    public void recordBroadcast(
            String type,
            String title,
            String body,
            String targetTopic,
            String deeplink,
            Map<String, String> payload
    ) {
        String payloadJson = null;
        if (payload != null && !payload.isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException ignored) {
                payloadJson = "{}";
            }
        }

        notificationInboxRepository.save(NotificationInboxEntity.builder()
                .type(type == null || type.isBlank() ? "general" : type)
                .title(title == null ? "알림" : title)
                .body(body == null ? "" : body)
                .targetTopic(targetTopic)
                .deeplink(deeplink)
                .payloadJson(payloadJson)
                .build());
    }

    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getInbox(String userId, int limit) {
        validateUserId(userId);

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<NotificationInboxEntity> all = notificationInboxRepository
                .findTop200ByCreatedAtAfterOrderByCreatedAtDesc(retentionCutoff());
        List<NotificationInboxEntity> notifications = all.stream().limit(safeLimit).toList();
        if (notifications.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = notifications.stream()
                .map(NotificationInboxEntity::getId)
                .collect(Collectors.toSet());

        Set<Long> readIds = notificationReadRepository.findByUserIdAndNotificationIdIn(userId, ids).stream()
                .map(NotificationReadEntity::getNotificationId)
                .collect(Collectors.toSet());

        return notifications.stream()
                .map(entity -> UserNotificationResponse.builder()
                        .id(entity.getId())
                        .type(entity.getType())
                        .title(entity.getTitle())
                        .body(entity.getBody())
                        .deeplink(entity.getDeeplink())
                        .targetTopic(entity.getTargetTopic())
                        .createdAt(entity.getCreatedAt())
                        .isRead(readIds.contains(entity.getId()))
                        .build())
                .toList();
    }

    @Transactional
    public void markRead(String userId, Long notificationId) {
        validateUserId(userId);
        purgeExpiredNotifications();
        notificationInboxRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (notificationReadRepository.existsByUserIdAndNotificationId(userId, notificationId)) {
            return;
        }

        notificationReadRepository.save(NotificationReadEntity.builder()
                .userId(userId)
                .notificationId(notificationId)
                .readAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public int markAllRead(String userId) {
        validateUserId(userId);
        purgeExpiredNotifications();

        List<NotificationInboxEntity> latest = notificationInboxRepository
                .findTop200ByCreatedAtAfterOrderByCreatedAtDesc(retentionCutoff());
        if (latest.isEmpty()) {
            return 0;
        }

        Set<Long> allIds = latest.stream()
                .map(NotificationInboxEntity::getId)
                .collect(Collectors.toSet());

        Set<Long> alreadyRead = notificationReadRepository.findByUserIdAndNotificationIdIn(userId, allIds).stream()
                .map(NotificationReadEntity::getNotificationId)
                .collect(Collectors.toSet());

        List<NotificationReadEntity> toSave = allIds.stream()
                .filter(id -> !alreadyRead.contains(id))
                .map(id -> NotificationReadEntity.builder()
                        .userId(userId)
                        .notificationId(id)
                        .readAt(LocalDateTime.now())
                        .build())
                .toList();

        if (!toSave.isEmpty()) {
            notificationReadRepository.saveAll(toSave);
        }
        return toSave.size();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        validateUserId(userId);

        List<NotificationInboxEntity> latest = notificationInboxRepository
                .findTop200ByCreatedAtAfterOrderByCreatedAtDesc(retentionCutoff());
        if (latest.isEmpty()) {
            return 0;
        }

        Set<Long> ids = latest.stream().map(NotificationInboxEntity::getId).collect(Collectors.toSet());
        long readCount = notificationReadRepository.countByUserIdAndNotificationIdIn(userId, ids);
        return Math.max(0, ids.size() - readCount);
    }

    @Transactional
    public void purgeExpiredNotifications() {
        LocalDateTime cutoff = retentionCutoff();
        List<NotificationInboxEntity> expired = notificationInboxRepository.findByCreatedAtBefore(cutoff);
        if (expired.isEmpty()) {
            return;
        }

        Set<Long> expiredIds = expired.stream()
                .map(NotificationInboxEntity::getId)
                .collect(Collectors.toSet());

        notificationReadRepository.deleteByNotificationIdIn(expiredIds);
        notificationInboxRepository.deleteByCreatedAtBefore(cutoff);
    }

    @Transactional(readOnly = true)
    public int retentionDays() {
        return Math.max(retentionDays, 1);
    }

    private LocalDateTime retentionCutoff() {
        return LocalDateTime.now().minusDays(Math.max(retentionDays, 1));
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}
