package com.snapfit.snapfitbackend.domain.notification.repository;

import com.snapfit.snapfitbackend.domain.notification.entity.NotificationInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationInboxRepository extends JpaRepository<NotificationInboxEntity, Long> {

    List<NotificationInboxEntity> findTop100ByOrderByCreatedAtDesc();

    List<NotificationInboxEntity> findTop200ByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime cutoff);

    List<NotificationInboxEntity> findByCreatedAtBefore(LocalDateTime cutoff);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
