package com.snapfit.snapfitbackend.domain.billing.repository;

import com.snapfit.snapfitbackend.domain.billing.entity.SubscriptionEntity;
import com.snapfit.snapfitbackend.domain.billing.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {
    long countByStatus(SubscriptionStatus status);
    long countByUpdatedAtAfter(LocalDateTime from);
}
