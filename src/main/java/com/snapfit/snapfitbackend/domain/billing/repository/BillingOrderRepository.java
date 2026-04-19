package com.snapfit.snapfitbackend.domain.billing.repository;

import com.snapfit.snapfitbackend.domain.billing.entity.BillingOrderEntity;
import com.snapfit.snapfitbackend.domain.billing.entity.BillingOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BillingOrderRepository extends JpaRepository<BillingOrderEntity, Long> {
    Optional<BillingOrderEntity> findByOrderId(String orderId);
    long countByCreatedAtAfter(LocalDateTime from);
    long countByStatusAndCreatedAtAfter(BillingOrderStatus status, LocalDateTime from);
    long countByStatusInAndCreatedAtAfter(Collection<BillingOrderStatus> statuses, LocalDateTime from);
    List<BillingOrderEntity> findByStatusInOrderByUpdatedAtDesc(
            Collection<BillingOrderStatus> statuses,
            Pageable pageable
    );

    long deleteByUserId(String userId);
}
