package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.notification.dto.UserNotificationResponse;
import com.snapfit.snapfitbackend.domain.notification.service.NotificationInboxService;
import com.snapfit.snapfitbackend.domain.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final PushNotificationService pushNotificationService;
    private final NotificationInboxService notificationInboxService;

    @Value("${snapfit.push.admin-key:}")
    private String adminKey;

    @PostMapping("/template-new")
    public ResponseEntity<?> sendTemplateNew(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestBody TemplateNewRequest request) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }
        String messageId = pushNotificationService.notifyTemplatePublished(request.templateId, request.templateTitle);
        return ResponseEntity.ok(Map.of("ok", true, "messageId", messageId));
    }

    @PostMapping("/order-status")
    public ResponseEntity<?> sendOrderStatus(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestBody OrderStatusRequest request) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }
        String messageId = pushNotificationService.notifyOrderStatus(
                request.orderId,
                request.status,
                request.messageBody);
        return ResponseEntity.ok(Map.of("ok", true, "messageId", messageId));
    }

    @PostMapping("/comment")
    public ResponseEntity<?> sendComment(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestBody CommentRequest request) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }
        String messageId = pushNotificationService.notifyComment(request.albumId, request.commenter, request.preview);
        return ResponseEntity.ok(Map.of("ok", true, "messageId", messageId));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }
        var result = pushNotificationService.healthCheckDryRun();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "topic", result.topic(),
                "messageId", result.messageId(),
                "attempts", result.attempts(),
                "durationMs", result.durationMs(),
                "dryRun", result.dryRun()));
    }

    @PostMapping("/topic")
    public ResponseEntity<?> sendToTopic(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestBody TopicPushRequest request) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }
        var result = pushNotificationService.sendToTopicWithRetryDetailed(
                request.topic,
                request.title,
                request.body,
                request.data == null ? Map.of() : request.data,
                request.dryRun);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "topic", result.topic(),
                "messageId", result.messageId(),
                "attempts", result.attempts(),
                "durationMs", result.durationMs(),
                "dryRun", result.dryRun()));
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<UserNotificationResponse>> getInbox(
            @RequestParam String userId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(notificationInboxService.getInbox(userId, limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(@RequestParam String userId) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationInboxService.unreadCount(userId)));
    }

    @GetMapping("/policy")
    public ResponseEntity<?> notificationPolicy() {
        return ResponseEntity.ok(Map.of(
                "retentionDays", notificationInboxService.retentionDays(),
                "description", "알림은 보관 기간 이후 자동 삭제됩니다."));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<?> markRead(
            @PathVariable Long notificationId,
            @RequestParam String userId) {
        notificationInboxService.markRead(userId, notificationId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(@RequestParam String userId) {
        int updated = notificationInboxService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("ok", true, "updated", updated));
    }

    private boolean isAuthorized(String key) {
        return adminKey != null && !adminKey.isBlank() && adminKey.equals(key);
    }

    public static class TemplateNewRequest {
        public Long templateId;
        public String templateTitle;
    }

    public static class OrderStatusRequest {
        public String orderId;
        public String status;
        public String messageBody;
    }

    public static class CommentRequest {
        public Long albumId;
        public String commenter;
        public String preview;
    }

    public static class TopicPushRequest {
        public String topic;
        public String title;
        public String body;
        public boolean dryRun = false;
        public Map<String, String> data;
    }
}
