package com.snapfit.snapfitbackend.domain.notification.repository;

import com.snapfit.snapfitbackend.domain.notification.entity.NotificationReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationReadEntity, Long> {

    List<NotificationReadEntity> findByUserIdAndNotificationIdIn(String userId, Collection<Long> notificationIds);

    boolean existsByUserIdAndNotificationId(String userId, Long notificationId);

    long countByUserIdAndNotificationIdIn(String userId, Collection<Long> notificationIds);

    long deleteByNotificationIdIn(Collection<Long> notificationIds);

    long deleteByUserId(String userId);
}
