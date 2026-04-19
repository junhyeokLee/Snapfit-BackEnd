package com.snapfit.snapfitbackend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationInboxService notificationInboxService;

    @Scheduled(cron = "${snapfit.notifications.cleanup-cron:0 10 3 * * *}", zone = "Asia/Seoul")
    public void cleanupExpiredNotifications() {
        notificationInboxService.purgeExpiredNotifications();
        log.debug("notification cleanup completed");
    }
}
