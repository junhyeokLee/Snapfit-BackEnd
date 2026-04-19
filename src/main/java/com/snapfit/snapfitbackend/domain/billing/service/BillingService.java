package com.snapfit.snapfitbackend.domain.billing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumPageEntity;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumPageRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumRepository;
import com.snapfit.snapfitbackend.domain.billing.dto.BillingPlanDto;
import com.snapfit.snapfitbackend.domain.billing.dto.PrepareBillingResponse;
import com.snapfit.snapfitbackend.domain.billing.dto.StoragePreflightResponse;
import com.snapfit.snapfitbackend.domain.billing.dto.StorageQuotaResponse;
import com.snapfit.snapfitbackend.domain.billing.dto.SubscriptionStatusResponse;
import com.snapfit.snapfitbackend.domain.billing.entity.BillingOrderEntity;
import com.snapfit.snapfitbackend.domain.billing.entity.BillingOrderStatus;
import com.snapfit.snapfitbackend.domain.billing.entity.BillingProvider;
import com.snapfit.snapfitbackend.domain.billing.entity.SubscriptionEntity;
import com.snapfit.snapfitbackend.domain.billing.entity.SubscriptionStatus;
import com.snapfit.snapfitbackend.domain.billing.repository.BillingOrderRepository;
import com.snapfit.snapfitbackend.domain.billing.repository.SubscriptionRepository;
import com.snapfit.snapfitbackend.domain.order.entity.OrderStatus;
import com.snapfit.snapfitbackend.domain.order.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    public static final String PLAN_PRO_MONTHLY = "SNAPFIT_PRO_MONTHLY";
    public static final String PLAN_FREE = "FREE";

    // URL 기반 추정 바이트(실측 메타 테이블 도입 전 계측용)
    private static final long EST_COVER_ORIGINAL_BYTES = 2_400_000L;
    private static final long EST_COVER_PREVIEW_BYTES = 350_000L;
    private static final long EST_COVER_THUMB_BYTES = 80_000L;
    private static final long EST_PAGE_ORIGINAL_BYTES = 2_000_000L;
    private static final long EST_PAGE_PREVIEW_BYTES = 300_000L;
    private static final long EST_PAGE_THUMB_BYTES = 60_000L;

    private final BillingOrderRepository billingOrderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AlbumRepository albumRepository;
    private final AlbumPageRepository albumPageRepository;
    private final OrderService orderService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${snapfit.billing.mock-mode:true}")
    private boolean mockMode;

    @Value("${snapfit.billing.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Value("${snapfit.billing.toss.secret-key:}")
    private String tossSecretKey;

    @Value("${snapfit.billing.toss.confirm-url:https://api.tosspayments.com/v1/payments/confirm}")
    private String tossConfirmUrl;

    @Value("${snapfit.billing.toss.cancel-url-prefix:https://api.tosspayments.com/v1/payments}")
    private String tossCancelUrlPrefix;

    @Value("${snapfit.billing.webhook.secret:}")
    private String webhookSecret;

    @Value("${snapfit.billing.webhook.toss.secret:}")
    private String tossWebhookSecret;

    @Value("${snapfit.billing.webhook.inicis.secret:}")
    private String inicisWebhookSecret;

    @Value("${snapfit.billing.storage.limit.free.soft-bytes:1073741824}")
    private long freeSoftLimitBytes;

    @Value("${snapfit.billing.storage.limit.free.hard-bytes:1073741824}")
    private long freeHardLimitBytes;

    @Value("${snapfit.billing.storage.limit.pro.soft-bytes:10737418240}")
    private long proSoftLimitBytes;

    @Value("${snapfit.billing.storage.limit.pro.hard-bytes:10737418240}")
    private long proHardLimitBytes;

    @PostConstruct
    public void validateSecurityConfig() {
        if (!mockMode && (tossSecretKey == null || tossSecretKey.isBlank())) {
            throw new IllegalStateException("SNAPFIT_BILLING_TOSS_SECRET_KEY is required when mock-mode=false");
        }
        if (!mockMode && (webhookSecret == null || webhookSecret.isBlank())
                && (tossWebhookSecret == null || tossWebhookSecret.isBlank())) {
            log.warn("billing webhook signature secret is empty. configure SNAPFIT_BILLING_WEBHOOK_SECRET or provider specific secret");
        }
    }

    @Transactional(readOnly = true)
    public List<BillingPlanDto> getPlans() {
        return List.of(
                BillingPlanDto.builder()
                        .planCode(PLAN_PRO_MONTHLY)
                        .title("SnapFit Pro 월간 구독 (Toss + NaverPay)")
                        .amount(4900)
                        .currency("KRW")
                        .periodDays(30)
                        .provider(BillingProvider.TOSS_NAVERPAY.name())
                        .build(),
                BillingPlanDto.builder()
                        .planCode(PLAN_PRO_MONTHLY)
                        .title("SnapFit Pro 월간 구독 (KG Inicis + NaverPay)")
                        .amount(4900)
                        .currency("KRW")
                        .periodDays(30)
                        .provider(BillingProvider.INICIS_NAVERPAY.name())
                        .build());
    }

    @Transactional
    public PrepareBillingResponse prepareSubscription(String userId, String planCode, String providerRaw) {
        validateUserId(userId);
        final BillingPlanDto plan = resolvePlan(planCode);
        final BillingProvider provider = BillingProvider.from(providerRaw);

        String orderId = buildOrderId(provider);
        LocalDateTime now = LocalDateTime.now();

        String checkoutUrl = buildCheckoutUrl(orderId, plan, provider);
        String successUrl = publicBaseUrl + "/api/billing/return/success?orderId=" + orderId;
        String failUrl = publicBaseUrl + "/api/billing/return/fail?orderId=" + orderId;

        BillingOrderEntity order = BillingOrderEntity.builder()
                .orderId(orderId)
                .userId(userId)
                .planCode(plan.getPlanCode())
                .provider(provider.name())
                .status(BillingOrderStatus.READY)
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .checkoutUrl(checkoutUrl)
                .build();
        billingOrderRepository.save(order);

        orderService.createOrReplaceBillingOrder(
                userId,
                orderId,
                "SnapFit Pro 월간 구독",
                plan.getAmount(),
                OrderStatus.PAYMENT_PENDING);

        return PrepareBillingResponse.builder()
                .orderId(orderId)
                .planCode(plan.getPlanCode())
                .provider(provider.name())
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .checkoutUrl(checkoutUrl)
                .successUrl(successUrl)
                .failUrl(failUrl)
                .expiresAt(now.plusMinutes(15))
                .isMock(mockMode)
                .build();
    }

    @Transactional
    public SubscriptionStatusResponse approveOrder(
            String orderId,
            String paymentKey,
            Integer amount,
            String reserveId,
            String transactionId
    ) {
        BillingOrderEntity order = billingOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == BillingOrderStatus.APPROVED) {
            return getSubscription(order.getUserId());
        }
        if (order.getStatus() != BillingOrderStatus.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not in READY state");
        }

        BillingProvider provider = BillingProvider.from(order.getProvider());
        if (provider == BillingProvider.TOSS_NAVERPAY) {
            confirmTossPayment(order, paymentKey, amount);
            order.setTransactionId(blankToNull(paymentKey));
        } else {
            if (!mockMode && (transactionId == null || transactionId.isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required for INICIS approve");
            }
            order.setTransactionId(blankToNull(transactionId));
        }

        order.setStatus(BillingOrderStatus.APPROVED);
        order.setReserveId(blankToNull(reserveId));
        order.setApprovedAt(LocalDateTime.now());
        billingOrderRepository.save(order);

        orderService.createOrReplaceBillingOrder(
                order.getUserId(),
                order.getOrderId(),
                "SnapFit Pro 월간 구독",
                order.getAmount(),
                OrderStatus.PAYMENT_COMPLETED);

        upsertActiveSubscription(order.getUserId(), order.getPlanCode(), order.getOrderId());
        return getSubscription(order.getUserId());
    }

    @Transactional
    public Map<String, Object> cancelPayment(String orderId, String cancelReason) {
        BillingOrderEntity order = billingOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == BillingOrderStatus.CANCELED) {
            return Map.of("ok", true, "status", BillingOrderStatus.CANCELED.name(), "orderId", orderId);
        }

        BillingProvider provider = BillingProvider.from(order.getProvider());
        if (!mockMode && provider == BillingProvider.TOSS_NAVERPAY) {
            if (order.getTransactionId() == null || order.getTransactionId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No paymentKey(transactionId) to cancel");
            }
            callTossCancel(order.getTransactionId(), cancelReason == null ? "USER_REQUEST" : cancelReason);
        }

        order.setStatus(BillingOrderStatus.CANCELED);
        order.setFailReason(cancelReason == null ? "CANCELED" : cancelReason);
        billingOrderRepository.save(order);
        orderService.setStatus(order.getOrderId(), OrderStatus.CANCELED);

        return Map.of("ok", true, "status", BillingOrderStatus.CANCELED.name(), "orderId", orderId);
    }

    @Transactional
    public Map<String, Object> handleWebhook(
            String providerRaw,
            String signature,
            String rawBody,
            String eventType,
            String orderId,
            String status,
            String paymentKey,
            String transactionId
    ) {
        BillingProvider provider = BillingProvider.from(providerRaw);
        verifyWebhookSignature(provider, signature, rawBody);

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }

        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(normalized) || "PAY_COMPLETED".equals(normalized) || "DONE".equals(normalized)) {
            SubscriptionStatusResponse response = approveOrder(orderId, paymentKey, null, null, transactionId);
            return Map.of("ok", true, "subscriptionStatus", response.getStatus(), "eventType", eventType, "provider", provider.name());
        }

        failOrder(orderId, normalized.isEmpty() ? "FAILED" : normalized);
        return Map.of("ok", true, "status", "FAILED", "eventType", eventType, "provider", provider.name());
    }

    @Transactional
    public Map<String, Object> runE2EScenario(String userId, String providerRaw, String paymentKey) {
        PrepareBillingResponse prepared = prepareSubscription(userId, PLAN_PRO_MONTHLY, providerRaw);

        if (!mockMode && (paymentKey == null || paymentKey.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "paymentKey is required for non-mock e2e. run real approval after frontend payment.");
        }

        String tx = mockMode ? "MOCK-PAY-" + System.currentTimeMillis() : paymentKey;
        SubscriptionStatusResponse approved = approveOrder(prepared.getOrderId(), tx, prepared.getAmount(), null, tx);

        orderService.setStatus(prepared.getOrderId(), OrderStatus.IN_PRODUCTION);
        orderService.setStatus(prepared.getOrderId(), OrderStatus.SHIPPING);
        var delivered = orderService.setStatus(prepared.getOrderId(), OrderStatus.DELIVERED);

        return Map.of(
                "ok", true,
                "orderId", prepared.getOrderId(),
                "provider", prepared.getProvider(),
                "subscriptionStatus", approved.getStatus(),
                "finalOrderStatus", delivered.getStatus());
    }

    @Transactional
    public void failOrder(String orderId, String reason) {
        Optional<BillingOrderEntity> maybeOrder = billingOrderRepository.findByOrderId(orderId);
        if (maybeOrder.isEmpty()) {
            log.warn("billing fail webhook for unknown orderId={}", orderId);
            return;
        }

        BillingOrderEntity order = maybeOrder.get();
        if (order.getStatus() == BillingOrderStatus.APPROVED) {
            return;
        }

        order.setStatus(BillingOrderStatus.FAILED);
        order.setFailReason(reason == null ? "PAYMENT_FAILED" : reason);
        billingOrderRepository.save(order);
        orderService.setStatus(order.getOrderId(), OrderStatus.CANCELED);
    }

    @Transactional
    public SubscriptionStatusResponse cancelSubscription(String userId) {
        validateUserId(userId);

        SubscriptionEntity sub = subscriptionRepository.findById(userId)
                .orElseGet(() -> SubscriptionEntity.builder()
                        .userId(userId)
                        .status(SubscriptionStatus.INACTIVE)
                        .build());

        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setExpiresAt(LocalDateTime.now());
        sub.setNextBillingAt(null);
        subscriptionRepository.save(sub);

        return toResponse(sub, userId);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscription(String userId) {
        validateUserId(userId);

        Optional<SubscriptionEntity> maybe = subscriptionRepository.findById(userId);
        if (maybe.isEmpty()) {
            return SubscriptionStatusResponse.builder()
                    .userId(userId)
                    .planCode(null)
                    .status(SubscriptionStatus.INACTIVE.name())
                    .isActive(false)
                    .build();
        }

        SubscriptionEntity sub = maybe.get();
        if (isExpired(sub)) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
        }

        return toResponse(sub, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        return subscriptionRepository.findById(userId)
                .map(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE && !isExpired(sub))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public StorageQuotaResponse getStorageQuota(String userId) {
        validateUserId(userId);

        final SubscriptionStatusResponse subscription = getSubscription(userId);
        final boolean isPro = subscription.isActive() && PLAN_PRO_MONTHLY.equals(subscription.getPlanCode());
        final String planCode = isPro ? PLAN_PRO_MONTHLY : PLAN_FREE;
        final long softLimit = isPro ? proSoftLimitBytes : freeSoftLimitBytes;
        final long hardLimit = isPro ? proHardLimitBytes : freeHardLimitBytes;

        final long usedBytes = estimateUserStorageBytes(userId);
        final boolean softExceeded = softLimit > 0 && usedBytes >= softLimit;
        final boolean hardExceeded = hardLimit > 0 && usedBytes >= hardLimit;
        final int usagePercent = hardLimit > 0
                ? (int) Math.min(999, (usedBytes * 100L) / hardLimit)
                : 0;

        return StorageQuotaResponse.builder()
                .userId(userId)
                .planCode(planCode)
                .usedBytes(usedBytes)
                .softLimitBytes(softLimit)
                .hardLimitBytes(hardLimit)
                .softExceeded(softExceeded)
                .hardExceeded(hardExceeded)
                .usagePercent(usagePercent)
                .measuredAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public StoragePreflightResponse preflightStorage(String userId, long incomingBytes) {
        validateUserId(userId);

        if (incomingBytes < 0) {
            throw new IllegalArgumentException("incomingBytes must be >= 0");
        }

        final StorageQuotaResponse quota = getStorageQuota(userId);
        final long projected = quota.getUsedBytes() + incomingBytes;
        final long hardLimit = quota.getHardLimitBytes();
        final boolean allowed = hardLimit <= 0 || projected <= hardLimit;
        final long remainingBytes = Math.max(0L, hardLimit - quota.getUsedBytes());
        final String reason = allowed ? "OK" : "HARD_LIMIT_EXCEEDED";

        return StoragePreflightResponse.builder()
                .userId(userId)
                .planCode(quota.getPlanCode())
                .incomingBytes(incomingBytes)
                .usedBytes(quota.getUsedBytes())
                .projectedBytes(projected)
                .hardLimitBytes(hardLimit)
                .remainingBytes(remainingBytes)
                .allowed(allowed)
                .reason(reason)
                .measuredAt(LocalDateTime.now())
                .build();
    }

    private void confirmTossPayment(BillingOrderEntity order, String paymentKey, Integer amount) {
        if (mockMode) {
            return;
        }

        if (paymentKey == null || paymentKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentKey is required");
        }

        int expectedAmount = order.getAmount();
        if (amount != null && amount != expectedAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount mismatch");
        }

        Map<String, Object> payload = Map.of(
                "paymentKey", paymentKey,
                "orderId", order.getOrderId(),
                "amount", expectedAmount);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(tossSecretKey, "", StandardCharsets.UTF_8);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(tossConfirmUrl, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "toss confirm failed");
        }

        try {
            Map<String, Object> json = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            String status = String.valueOf(json.getOrDefault("status", ""));
            int totalAmount = ((Number) json.getOrDefault("totalAmount", 0)).intValue();

            if (!"DONE".equalsIgnoreCase(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payment not completed: " + status);
            }
            if (totalAmount != expectedAmount) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "confirmed amount mismatch");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid toss confirm response");
        }
    }

    private void callTossCancel(String paymentKey, String cancelReason) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(tossSecretKey, "", StandardCharsets.UTF_8);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                Map.of("cancelReason", cancelReason == null ? "USER_REQUEST" : cancelReason),
                headers);

        String url = tossCancelUrlPrefix + "/" + paymentKey + "/cancel";
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "toss cancel failed");
        }
    }

    private void verifyWebhookSignature(BillingProvider provider, String signature, String rawBody) {
        if (mockMode) {
            return;
        }

        String secret = switch (provider) {
            case TOSS_NAVERPAY -> firstNonBlank(tossWebhookSecret, webhookSecret);
            case INICIS_NAVERPAY -> firstNonBlank(inicisWebhookSecret, webhookSecret);
        };

        if (secret == null || secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Webhook secret is not configured for provider=" + provider.name());
        }
        if (signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing webhook signature");
        }

        String expected = hmacSha256Hex(secret, rawBody == null ? "" : rawBody);
        if (!expected.equalsIgnoreCase(signature.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "HMAC calculation failed");
        }
    }

    private String buildOrderId(BillingProvider provider) {
        String prefix = provider == BillingProvider.INICIS_NAVERPAY ? "IC" : "TS";
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private BillingPlanDto resolvePlan(String planCode) {
        String code = (planCode == null || planCode.isBlank()) ? PLAN_PRO_MONTHLY : planCode;
        return getPlans().stream()
                .filter(p -> p.getPlanCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown plan: " + code));
    }

    private String buildCheckoutUrl(String orderId, BillingPlanDto plan, BillingProvider provider) {
        if (mockMode) {
            return publicBaseUrl + "/api/billing/mock/checkout?orderId=" + orderId + "&amount=" + plan.getAmount();
        }

        return publicBaseUrl + "/api/billing/checkout/naverpay?orderId=" + orderId + "&provider=" + provider.name();
    }

    private void upsertActiveSubscription(String userId, String planCode, String orderId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.plusDays(30);

        SubscriptionEntity sub = subscriptionRepository.findById(userId)
                .orElseGet(() -> SubscriptionEntity.builder().userId(userId).build());

        sub.setPlanCode(planCode);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartedAt(now);
        sub.setExpiresAt(next);
        sub.setNextBillingAt(next);
        sub.setLastOrderId(orderId);
        subscriptionRepository.save(sub);
    }

    private boolean isExpired(SubscriptionEntity sub) {
        return sub.getExpiresAt() != null && sub.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private SubscriptionStatusResponse toResponse(SubscriptionEntity sub, String userId) {
        boolean active = sub.getStatus() == SubscriptionStatus.ACTIVE && !isExpired(sub);
        return SubscriptionStatusResponse.builder()
                .userId(userId)
                .planCode(sub.getPlanCode())
                .status((active ? SubscriptionStatus.ACTIVE : sub.getStatus()).name())
                .startedAt(sub.getStartedAt())
                .expiresAt(sub.getExpiresAt())
                .nextBillingAt(sub.getNextBillingAt())
                .isActive(active)
                .build();
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private long estimateUserStorageBytes(String userId) {
        final List<AlbumEntity> albums = albumRepository.findByUserIdOrderByOrdersAscCreatedAtDesc(userId);
        long totalBytes = 0L;

        for (AlbumEntity album : albums) {
            totalBytes += estimateCoverBytes(album);
            totalBytes += estimatePageBytes(album.getId());
        }
        return Math.max(0L, totalBytes);
    }

    private long estimateCoverBytes(AlbumEntity album) {
        long sum = 0L;
        if (hasText(album.getCoverOriginalUrl())) {
            sum += EST_COVER_ORIGINAL_BYTES;
        }
        if (hasText(album.getCoverPreviewUrl()) || hasText(album.getCoverImageUrl())) {
            sum += EST_COVER_PREVIEW_BYTES;
        }
        if (hasText(album.getCoverThumbnailUrl())) {
            sum += EST_COVER_THUMB_BYTES;
        }
        return sum;
    }

    private long estimatePageBytes(Long albumId) {
        if (albumId == null) {
            return 0L;
        }
        long sum = 0L;
        final List<AlbumPageEntity> pages = albumPageRepository.findByAlbumId(albumId);
        for (AlbumPageEntity page : pages) {
            if (hasText(page.getOriginalUrl())) {
                sum += EST_PAGE_ORIGINAL_BYTES;
            }
            if (hasText(page.getPreviewUrl()) || hasText(page.getImageUrl())) {
                sum += EST_PAGE_PREVIEW_BYTES;
            }
            if (hasText(page.getThumbnailUrl())) {
                sum += EST_PAGE_THUMB_BYTES;
            }
        }
        return sum;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
