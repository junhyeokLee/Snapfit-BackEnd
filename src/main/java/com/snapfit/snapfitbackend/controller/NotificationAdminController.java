package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final PushNotificationService pushNotificationService;

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
}
