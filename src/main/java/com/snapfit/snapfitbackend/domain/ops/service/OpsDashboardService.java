package com.snapfit.snapfitbackend.domain.ops.service;

import com.snapfit.snapfitbackend.domain.auth.repository.UserRepository;
import com.snapfit.snapfitbackend.domain.billing.entity.BillingOrderStatus;
import com.snapfit.snapfitbackend.domain.billing.entity.SubscriptionStatus;
import com.snapfit.snapfitbackend.domain.billing.entity.BillingOrderEntity;
import com.snapfit.snapfitbackend.domain.billing.repository.BillingOrderRepository;
import com.snapfit.snapfitbackend.domain.billing.repository.SubscriptionRepository;
import com.snapfit.snapfitbackend.domain.order.entity.OrderEntity;
import com.snapfit.snapfitbackend.domain.order.entity.OrderStatus;
import com.snapfit.snapfitbackend.domain.order.repository.OrderRepository;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpsDashboardService {

    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;
    private final OrderRepository orderRepository;
    private final BillingOrderRepository billingOrderRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from24h = now.minusHours(24);
        LocalDateTime from7d = now.minusDays(7);

        long usersTotal = userRepository.count();
        long users24h = userRepository.countByCreatedAtAfter(from24h);
        long users7d = userRepository.countByCreatedAtAfter(from7d);

        long templatesTotal = templateRepository.count();
        long templatesActive = templateRepository.countActive();
        long templatesInactive = templateRepository.countByActiveFalse();

        long orderPending = orderRepository.countByStatus(OrderStatus.PAYMENT_PENDING);
        long orderPaid = orderRepository.countByStatus(OrderStatus.PAYMENT_COMPLETED);
        long orderProd = orderRepository.countByStatus(OrderStatus.IN_PRODUCTION);
        long orderShip = orderRepository.countByStatus(OrderStatus.SHIPPING);
        long orderDone = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long orderCanceled = orderRepository.countByStatus(OrderStatus.CANCELED);
        long orderTotal = orderPending + orderPaid + orderProd + orderShip + orderDone + orderCanceled;
        long order24h = orderRepository.countByCreatedAtAfter(from24h);

        long billing24h = billingOrderRepository.countByCreatedAtAfter(from24h);
        long billingReady24h = billingOrderRepository.countByStatusAndCreatedAtAfter(BillingOrderStatus.READY, from24h);
        long billingApproved24h = billingOrderRepository.countByStatusAndCreatedAtAfter(BillingOrderStatus.APPROVED, from24h);
        long billingFailed24h = billingOrderRepository.countByStatusAndCreatedAtAfter(BillingOrderStatus.FAILED, from24h);
        long billingCanceled24h = billingOrderRepository.countByStatusAndCreatedAtAfter(BillingOrderStatus.CANCELED, from24h);

        long subActive = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long subInactive = subscriptionRepository.countByStatus(SubscriptionStatus.INACTIVE);
        long subCanceled = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED);
        long subExpired = subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED);
        long subChanged24h = subscriptionRepository.countByUpdatedAtAfter(from24h);

        return Map.of(
                "generatedAt", now,
                "users", Map.of(
                        "total", usersTotal,
                        "new24h", users24h,
                        "new7d", users7d
                ),
                "templates", Map.of(
                        "total", templatesTotal,
                        "active", templatesActive,
                        "inactive", templatesInactive
                ),
                "orders", Map.of(
                        "total", orderTotal,
                        "new24h", order24h,
                        "paymentPending", orderPending,
                        "paymentCompleted", orderPaid,
                        "inProduction", orderProd,
                        "shipping", orderShip,
                        "delivered", orderDone,
                        "canceled", orderCanceled
                ),
                "billing", Map.of(
                        "created24h", billing24h,
                        "ready24h", billingReady24h,
                        "approved24h", billingApproved24h,
                        "failed24h", billingFailed24h,
                        "canceled24h", billingCanceled24h
                ),
                "subscriptions", Map.of(
                        "active", subActive,
                        "inactive", subInactive,
                        "canceled", subCanceled,
                        "expired", subExpired,
                        "changed24h", subChanged24h
                )
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> csSignals(int limit) {
        final int safeLimit = Math.min(Math.max(limit, 10), 200);
        final var pageable = PageRequest.of(0, safeLimit);

        List<OrderEntity> orderIssues = orderRepository.findByStatusInOrderByUpdatedAtDesc(
                List.of(OrderStatus.CANCELED, OrderStatus.PAYMENT_PENDING),
                pageable);
        List<BillingOrderEntity> billingIssues = billingOrderRepository.findByStatusInOrderByUpdatedAtDesc(
                List.of(BillingOrderStatus.FAILED, BillingOrderStatus.CANCELED),
                pageable);

        List<Map<String, Object>> logs = new ArrayList<>();

        for (OrderEntity order : orderIssues) {
            logs.add(Map.of(
                    "type", "ORDER",
                    "severity", order.getStatus() == OrderStatus.CANCELED ? "HIGH" : "MEDIUM",
                    "code", order.getStatus().name(),
                    "title", "주문 이슈",
                    "message", "%s 상태 주문 확인 필요".formatted(order.getStatus().name()),
                    "orderId", order.getOrderId(),
                    "userId", order.getUserId(),
                    "updatedAt", order.getUpdatedAt() == null ? order.getCreatedAt() : order.getUpdatedAt()));
        }

        for (BillingOrderEntity billing : billingIssues) {
            String reason = billing.getFailReason() == null || billing.getFailReason().isBlank()
                    ? "결제 처리 실패/취소"
                    : billing.getFailReason();
            logs.add(Map.of(
                    "type", "BILLING",
                    "severity", billing.getStatus() == BillingOrderStatus.FAILED ? "HIGH" : "MEDIUM",
                    "code", billing.getStatus().name(),
                    "title", "결제 이슈",
                    "message", reason,
                    "orderId", billing.getOrderId(),
                    "userId", billing.getUserId(),
                    "updatedAt", billing.getUpdatedAt() == null ? billing.getCreatedAt() : billing.getUpdatedAt()));
        }

        logs.sort(Comparator.comparing(
                (Map<String, Object> row) -> (LocalDateTime) row.get("updatedAt"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (logs.size() > safeLimit) {
            logs = logs.subList(0, safeLimit);
        }

        return Map.of(
                "count", logs.size(),
                "items", logs);
    }
}
