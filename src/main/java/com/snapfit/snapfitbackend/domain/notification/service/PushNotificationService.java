package com.snapfit.snapfitbackend.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class PushNotificationService {

    public static final String TOPIC_ORDER = "snapfit_order_updates";
    public static final String TOPIC_INVITE = "snapfit_invite_updates";
    public static final String TOPIC_COMMENT = "snapfit_comment_updates";
    public static final String TOPIC_TEMPLATE_NEW = "snapfit_template_new";
    private final NotificationInboxService notificationInboxService;

    public record PushSendResult(
            String topic,
            String messageId,
            int attempts,
            long durationMs,
            boolean dryRun) {
    }

    public String notifyTemplatePublished(Long templateId, String templateTitle) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "template_new");
        data.put("templateId", String.valueOf(templateId));
        return sendToTopicWithRetry(
                TOPIC_TEMPLATE_NEW,
                "새 템플릿이 등록됐어요",
                templateTitle + " 템플릿을 확인해보세요.",
                data);
    }

    public String notifyInviteCreated(Long albumId, String albumTitle, String inviteToken) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "album_invite");
        data.put("albumId", String.valueOf(albumId));
        data.put("inviteToken", inviteToken);
        return sendToTopicWithRetry(
                TOPIC_INVITE,
                "공유 앨범 초대가 도착했어요",
                albumTitle + " 앨범에 초대되었습니다.",
                data);
    }

    public String notifyInviteAccepted(Long albumId, String albumTitle, String userId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "invite_accepted");
        data.put("albumId", String.valueOf(albumId));
        data.put("userId", userId);
        return sendToTopicWithRetry(
                TOPIC_INVITE,
                "공유 멤버가 참여했어요",
                albumTitle + " 앨범 초대가 수락되었습니다.",
                data);
    }

    public String notifyOrderStatus(String orderId, String status, String messageBody) {
        return notifyOrderStatus(orderId, status, messageBody, null, null, null);
    }

    public String notifyOrderStatus(
            String orderId,
            String status,
            String messageBody,
            String deeplink,
            String courier,
            String trackingNumber) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "order_status");
        data.put("orderId", orderId);
        data.put("status", status);
        if (deeplink != null && !deeplink.isBlank()) {
            data.put("deeplink", deeplink);
        }
        if (courier != null && !courier.isBlank()) {
            data.put("courier", courier);
        }
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            data.put("trackingNumber", trackingNumber);
        }
        return sendToTopicWithRetry(
                TOPIC_ORDER,
                "주문 상태가 변경되었어요",
                messageBody,
                data);
    }

    public String notifyComment(Long albumId, String commenter, String preview) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "album_comment");
        data.put("albumId", String.valueOf(albumId));
        data.put("commenter", commenter);
        return sendToTopicWithRetry(
                TOPIC_COMMENT,
                commenter + "님이 댓글을 남겼어요",
                preview,
                data);
    }

    public String sendToTopicWithRetry(String topic, String title, String body, Map<String, String> data) {
        return sendToTopicWithRetryDetailed(topic, title, body, data, false).messageId();
    }

    public PushSendResult sendToTopicWithRetryDetailed(
            String topic,
            String title,
            String body,
            Map<String, String> data,
            boolean dryRun) {
        final int maxAttempts = 3;
        long backoffMs = 500L;
        final long startedAt = System.currentTimeMillis();
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Message message = Message.builder()
                        .setTopic(Objects.requireNonNull(topic))
                        .setNotification(Notification.builder()
                                .setTitle(title == null ? "" : title)
                                .setBody(body == null ? "" : body)
                                .build())
                        .putAllData(data == null ? Map.of() : data)
                        .build();
                String messageId = FirebaseMessaging.getInstance().send(message, dryRun);
                long duration = System.currentTimeMillis() - startedAt;
                if (!dryRun) {
                    notificationInboxService.recordBroadcast(
                            data == null ? "general" : data.getOrDefault("type", "general"),
                            title,
                            body,
                            topic,
                            data == null ? null : data.get("deeplink"),
                            data == null ? Map.of() : data);
                }
                log.info("FCM sent. topic={}, attempt={}, dryRun={}, durationMs={}, messageId={}",
                        topic, attempt, dryRun, duration, messageId);
                return new PushSendResult(topic, messageId, attempt, duration, dryRun);
            } catch (FirebaseMessagingException e) {
                log.error("FCM send failed. topic={}, attempt={}, code={}, msg={}",
                        topic, attempt, e.getErrorCode(), e.getMessage());
                lastException = new RuntimeException("FCM send failed: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("FCM send failed. topic={}, attempt={}", topic, attempt, e);
                lastException = new RuntimeException("FCM send failed: " + e.getMessage(), e);
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("FCM retry interrupted", ie);
                }
                backoffMs *= 2;
            }
        }

        throw lastException == null ? new RuntimeException("FCM send failed") : lastException;
    }

    public PushSendResult healthCheckDryRun() {
        Map<String, String> data = Map.of(
                "type", "health_check",
                "source", "snapfit-backend");
        return sendToTopicWithRetryDetailed(
                TOPIC_TEMPLATE_NEW,
                "[DryRun] SnapFit Push Health",
                "FCM 자격/전송 경로 점검",
                data,
                true);
    }
}
