package com.snapfit.snapfitbackend.domain.order.repository;

import com.snapfit.snapfitbackend.domain.order.entity.OrderEntity;
import com.snapfit.snapfitbackend.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<OrderEntity> findByUserIdAndStatusInOrderByCreatedAtDesc(
            String userId,
            Collection<OrderStatus> statuses,
            Pageable pageable
    );

    Optional<OrderEntity> findByOrderId(String orderId);
    Optional<OrderEntity> findFirstByUserIdAndAlbumIdAndStatusOrderByCreatedAtDesc(
            String userId,
            Long albumId,
            OrderStatus status
    );
    Optional<OrderEntity> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            String userId,
            OrderStatus status
    );
    Optional<OrderEntity> findFirstByUserIdOrderByUpdatedAtDesc(String userId);

    long countByUserIdAndStatus(String userId, OrderStatus status);
    long countByStatus(OrderStatus status);
    long countByCreatedAtAfter(LocalDateTime from);

    @Query("""
            select o
            from OrderEntity o
            where
              (
                :keyword is null or :keyword = '' or
                lower(o.orderId) like lower(concat('%', :keyword, '%')) or
                lower(o.userId) like lower(concat('%', :keyword, '%')) or
                lower(coalesce(o.recipientName, '')) like lower(concat('%', :keyword, '%')) or
                lower(coalesce(o.recipientPhone, '')) like lower(concat('%', :keyword, '%'))
              )
              and
              (
                :statusesEmpty = true or o.status in :statuses
              )
            order by o.createdAt desc
            """)
    Page<OrderEntity> searchAdminOrders(
            @Param("keyword") String keyword,
            @Param("statusesEmpty") boolean statusesEmpty,
            @Param("statuses") Collection<OrderStatus> statuses,
            Pageable pageable
    );

    List<OrderEntity> findByStatusInOrderByUpdatedAtDesc(
            Collection<OrderStatus> statuses,
            Pageable pageable
    );

    long deleteByUserId(String userId);
}
