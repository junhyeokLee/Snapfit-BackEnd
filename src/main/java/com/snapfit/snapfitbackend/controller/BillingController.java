package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.billing.dto.BillingPlanDto;
import com.snapfit.snapfitbackend.domain.billing.dto.PrepareBillingResponse;
import com.snapfit.snapfitbackend.domain.billing.dto.StoragePreflightRequest;
import com.snapfit.snapfitbackend.domain.billing.dto.StoragePreflightResponse;
import com.snapfit.snapfitbackend.domain.billing.dto.StorageQuotaResponse;
import com.snapfit.snapfitbackend.domain.billing.dto.SubscriptionStatusResponse;
import com.snapfit.snapfitbackend.domain.billing.entity.BillingProvider;
import com.snapfit.snapfitbackend.domain.billing.service.BillingReadinessService;
import com.snapfit.snapfitbackend.domain.billing.service.BillingService;
import com.snapfit.snapfitbackend.global.security.InMemoryRequestGuard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final BillingService billingService;
    private final BillingReadinessService billingReadinessService;
    private final ObjectMapper objectMapper;
    private final InMemoryRequestGuard requestGuard;
    private final Environment environment;

    @Value("${snapfit.order.admin-key:}")
    private String orderAdminKey;

    @GetMapping("/plans")
    public ResponseEntity<List<BillingPlanDto>> plans() {
        return ResponseEntity.ok(billingService.getPlans());
    }

    @GetMapping("/admin/readiness")
    public ResponseEntity<Map<String, Object>> readiness(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        ensureOrderAdmin(adminKey);
        return ResponseEntity.ok(billingReadinessService.readiness());
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionStatusResponse> getSubscription(@RequestParam String userId) {
        return ResponseEntity.ok(billingService.getSubscription(userId));
    }

    @PostMapping("/subscription/cancel")
    public ResponseEntity<SubscriptionStatusResponse> cancelSubscription(@RequestParam String userId) {
        return ResponseEntity.ok(billingService.cancelSubscription(userId));
    }

    @GetMapping("/storage/quota")
    public ResponseEntity<StorageQuotaResponse> getStorageQuota(@RequestParam String userId) {
        return ResponseEntity.ok(billingService.getStorageQuota(userId));
    }

    @PostMapping("/storage/preflight")
    public ResponseEntity<StoragePreflightResponse> preflightStorage(@RequestBody StoragePreflightRequest request) {
        return ResponseEntity.ok(
                billingService.preflightStorage(
                        request.getUserId(),
                        request.getIncomingBytes()));
    }

    @PostMapping("/prepare")
    public ResponseEntity<PrepareBillingResponse> prepare(@RequestBody PrepareRequest request) {
        return ResponseEntity.ok(
                billingService.prepareSubscription(
                        request.userId,
                        request.planCode,
                        request.provider));
    }

    @PostMapping("/approve")
    public ResponseEntity<SubscriptionStatusResponse> approve(@RequestBody ApproveRequest request) {
        return ResponseEntity.ok(
                billingService.approveOrder(
                        request.orderId,
                        request.paymentKey,
                        request.amount,
                        request.reserveId,
                        request.transactionId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable String orderId,
            @RequestBody(required = false) CancelRequest request
    ) {
        String reason = request == null ? null : request.reason;
        return ResponseEntity.ok(billingService.cancelPayment(orderId, reason));
    }

    @PostMapping("/webhook/{provider}")
    public ResponseEntity<Map<String, Object>> webhookByProvider(
            @PathVariable String provider,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawBody,
            HttpServletRequest http
    ) throws Exception {
        String ip = clientIp(http);
        if (!requestGuard.allowRate("billing:webhook:" + provider + ":" + ip, 120, 60)) {
            log.warn("rate-limit billing-webhook blocked provider={} ip={}", provider, ip);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many webhook requests");
        }
        Map<String, Object> json = objectMapper.readValue(rawBody, new TypeReference<>() {
        });
        String eventType = asString(json.get("eventType"));
        String orderId = asString(firstNonNull(json.get("orderId"), json.get("orderNo")));
        String status = asString(firstNonNull(json.get("status"), json.get("paymentStatus")));
        String paymentKey = asString(firstNonNull(json.get("paymentKey"), json.get("tid")));
        String transactionId = asString(firstNonNull(json.get("transactionId"), json.get("tid")));
        String replayKey = webhookReplayKey(provider, orderId, eventType, status, transactionId, paymentKey, signature, rawBody);
        if (!requestGuard.tryRegisterReplayKey(replayKey, 600)) {
            log.info("billing-webhook replay ignored provider={} orderId={} eventType={} status={}", provider, orderId, eventType, status);
            return ResponseEntity.ok(Map.of("ok", true, "replayed", true, "provider", provider));
        }
        log.info("billing-webhook accepted provider={} orderId={} eventType={} status={}", provider, orderId, eventType, status);

        return ResponseEntity.ok(billingService.handleWebhook(
                provider,
                signature,
                rawBody,
                eventType,
                orderId,
                status,
                paymentKey,
                transactionId));
    }

    @PostMapping("/test/e2e-run")
    public ResponseEntity<Map<String, Object>> runE2E(@RequestBody E2ERunRequest request) {
        ensureNonProdTestApi();
        return ResponseEntity.ok(
                billingService.runE2EScenario(
                        request.userId,
                        request.provider == null ? BillingProvider.TOSS_NAVERPAY.name() : request.provider,
                        request.paymentKey));
    }

    @PostMapping("/naverpay/prepare")
    public ResponseEntity<PrepareBillingResponse> prepareNaverPay(@RequestBody PrepareNaverPayRequest request) {
        return ResponseEntity.ok(
                billingService.prepareSubscription(
                        request.userId,
                        request.planCode,
                        request.provider == null ? BillingProvider.TOSS_NAVERPAY.name() : request.provider));
    }

    @PostMapping("/naverpay/approve")
    public ResponseEntity<SubscriptionStatusResponse> approveNaverPay(@RequestBody ApproveNaverPayRequest request) {
        return ResponseEntity.ok(
                billingService.approveOrder(
                        request.orderId,
                        request.paymentKey,
                        request.amount,
                        request.reserveId,
                        request.transactionId));
    }

    @PostMapping("/naverpay/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawBody,
            HttpServletRequest http
    ) throws Exception {
        String ip = clientIp(http);
        if (!requestGuard.allowRate("billing:webhook:naverpay:" + ip, 120, 60)) {
            log.warn("rate-limit naverpay-webhook blocked ip={}", ip);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many webhook requests");
        }
        Map<String, Object> json = objectMapper.readValue(rawBody, new TypeReference<>() {
        });
        NaverPayWebhookRequest request = objectMapper.convertValue(json, NaverPayWebhookRequest.class);
        String provider = request.provider == null ? BillingProvider.TOSS_NAVERPAY.name() : request.provider;
        String replayKey = webhookReplayKey(
                provider,
                request.orderId,
                request.eventType,
                request.status,
                request.transactionId,
                request.paymentKey,
                signature,
                rawBody
        );
        if (!requestGuard.tryRegisterReplayKey(replayKey, 600)) {
            log.info("naverpay-webhook replay ignored provider={} orderId={} eventType={} status={}", provider, request.orderId, request.eventType, request.status);
            return ResponseEntity.ok(Map.of("ok", true, "replayed", true, "provider", provider));
        }
        log.info("naverpay-webhook accepted provider={} orderId={} eventType={} status={}", provider, request.orderId, request.eventType, request.status);
        return ResponseEntity.ok(
                billingService.handleWebhook(
                        provider,
                        signature,
                        rawBody,
                        request.eventType,
                        request.orderId,
                        request.status,
                        request.paymentKey,
                        request.transactionId));
    }

    @GetMapping(value = "/mock/checkout", produces = MediaType.TEXT_HTML_VALUE)
    public String mockCheckout(
            @RequestParam String orderId,
            @RequestParam(required = false, defaultValue = "4900") String amount
    ) {
        return """
                <!DOCTYPE html>
                <html lang=\"ko\">
                <head>
                  <meta charset=\"UTF-8\" />
                  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                  <title>SnapFit NaverPay Checkout</title>
                  <style>
                    body {font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background:#f4f8fb; padding:24px;}
                    .card {max-width:460px; margin:0 auto; background:#fff; border-radius:16px; padding:20px; box-shadow:0 12px 30px rgba(0,0,0,.08);}
                    h1 {font-size:20px; margin:0 0 6px;}
                    .meta {color:#556; font-size:14px; margin-bottom:16px;}
                    button {width:100%; border:0; border-radius:12px; padding:14px 16px; font-size:15px; font-weight:700; cursor:pointer;}
                    .approve {background:#03c75a; color:#fff; margin-top:8px;}
                    .fail {background:#eef1f5; color:#344; margin-top:10px;}
                    .msg {margin-top:14px; font-size:13px; color:#4a5568;}
                  </style>
                </head>
                <body>
                  <div class=\"card\">
                    <h1>네이버페이 결제 테스트</h1>
                    <div class=\"meta\">주문번호: %s<br/>결제금액: %s원</div>
                    <button class=\"approve\" onclick=\"approve()\">결제 승인</button>
                    <button class=\"fail\" onclick=\"fail()\">결제 실패</button>
                    <div class=\"msg\" id=\"msg\">버튼을 누르면 구독 상태가 즉시 갱신됩니다.</div>
                  </div>

                  <script>
                    async function approve() {
                      const res = await fetch('/api/billing/naverpay/approve', {
                        method:'POST',
                        headers:{'Content-Type':'application/json'},
                        body: JSON.stringify({ orderId: '%s', transactionId: 'MOCK-TX-' + Date.now() })
                      });
                      document.getElementById('msg').innerText = res.ok
                        ? '결제가 승인되었습니다. 앱으로 돌아가서 구독 상태를 새로고침해주세요.'
                        : '결제 승인에 실패했습니다.';
                    }

                    async function fail() {
                      await fetch('/api/billing/naverpay/webhook', {
                        method:'POST',
                        headers:{'Content-Type':'application/json'},
                        body: JSON.stringify({ eventType: 'payment', orderId: '%s', status: 'FAILED' })
                      });
                      document.getElementById('msg').innerText = '실패 처리되었습니다. 다시 시도해주세요.';
                    }
                  </script>
                </body>
                </html>
                """.formatted(orderId, amount, orderId, orderId);
    }

    @GetMapping(value = "/return/success", produces = MediaType.TEXT_HTML_VALUE)
    public String returnSuccess(
            @RequestParam String orderId,
            @RequestParam(required = false) String paymentKey,
            @RequestParam(required = false) Integer amount
    ) {
        String deeplink = "snapfit://billing/success?orderId=" + enc(orderId)
                + (paymentKey == null ? "" : "&paymentKey=" + enc(paymentKey))
                + (amount == null ? "" : "&amount=" + amount);

        return """
                <!DOCTYPE html>
                <html lang=\"ko\">
                <head><meta charset=\"UTF-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                  <title>결제 완료</title>
                  <style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;padding:24px;background:#f4f8fb}.card{max-width:480px;margin:0 auto;background:#fff;border-radius:14px;padding:18px;box-shadow:0 8px 24px rgba(0,0,0,.08)}button{width:100%%;padding:12px;border:0;border-radius:10px;background:#07b8de;color:#fff;font-weight:700;cursor:pointer}</style>
                </head>
                <body>
                <div class=\"card\">
                  <h2>결제가 완료되었습니다</h2>
                  <p>앱으로 자동 복귀합니다. 복귀가 안 되면 아래 버튼을 눌러주세요.</p>
                  <button onclick=\"go()\">SnapFit 앱으로 돌아가기</button>
                </div>
                <script>
                  const url = '%s';
                  function go(){ window.location.href = url; }
                  setTimeout(go, 150);
                </script>
                </body>
                </html>
                """.formatted(deeplink);
    }

    @GetMapping(value = "/return/fail", produces = MediaType.TEXT_HTML_VALUE)
    public String returnFail(
            @RequestParam String orderId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String message
    ) {
        String deeplink = "snapfit://billing/fail?orderId=" + enc(orderId)
                + (code == null ? "" : "&code=" + enc(code))
                + (message == null ? "" : "&message=" + enc(message));

        return """
                <!DOCTYPE html>
                <html lang=\"ko\">
                <head><meta charset=\"UTF-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                  <title>결제 실패</title>
                  <style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;padding:24px;background:#fef6f6}.card{max-width:480px;margin:0 auto;background:#fff;border-radius:14px;padding:18px;box-shadow:0 8px 24px rgba(0,0,0,.08)}button{width:100%%;padding:12px;border:0;border-radius:10px;background:#dd4b39;color:#fff;font-weight:700;cursor:pointer}</style>
                </head>
                <body>
                <div class=\"card\">
                  <h2>결제에 실패했습니다</h2>
                  <p>앱으로 돌아가 결제를 다시 시도해주세요.</p>
                  <button onclick=\"go()\">SnapFit 앱으로 돌아가기</button>
                </div>
                <script>
                  const url = '%s';
                  function go(){ window.location.href = url; }
                  setTimeout(go, 150);
                </script>
                </body>
                </html>
                """.formatted(deeplink);
    }

    @Data
    public static class PrepareNaverPayRequest {
        public String userId;
        public String planCode;
        public String provider;
    }

    @Data
    public static class ApproveNaverPayRequest {
        public String orderId;
        public Integer amount;
        public String paymentKey;
        public String reserveId;
        public String transactionId;
    }

    @Data
    public static class NaverPayWebhookRequest {
        public String provider;
        public String eventType;
        public String orderId;
        public String status;
        public String paymentKey;
        public String transactionId;
    }

    @Data
    public static class PrepareRequest {
        public String userId;
        public String planCode;
        public String provider;
    }

    @Data
    public static class ApproveRequest {
        public String orderId;
        public Integer amount;
        public String paymentKey;
        public String reserveId;
        public String transactionId;
    }

    @Data
    public static class CancelRequest {
        public String reason;
    }

    @Data
    public static class E2ERunRequest {
        public String userId;
        public String provider;
        public String paymentKey;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private Object firstNonNull(Object a, Object b) {
        return a != null ? a : b;
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String clientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            int comma = xf.indexOf(',');
            return (comma >= 0 ? xf.substring(0, comma) : xf).trim();
        }
        String xr = request.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) return xr.trim();
        return request.getRemoteAddr();
    }

    private String webhookReplayKey(
            String provider,
            String orderId,
            String eventType,
            String status,
            String transactionId,
            String paymentKey,
            String signature,
            String rawBody
    ) {
        String raw = String.join("|",
                provider == null ? "" : provider,
                orderId == null ? "" : orderId,
                eventType == null ? "" : eventType,
                status == null ? "" : status,
                transactionId == null ? "" : transactionId,
                paymentKey == null ? "" : paymentKey,
                signature == null ? "" : signature,
                sha256(rawBody == null ? "" : rawBody));
        return "billing:replay:" + sha256(raw);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private void ensureNonProdTestApi() {
        if (environment.matchesProfiles("prod")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void ensureOrderAdmin(String key) {
        if (orderAdminKey == null || orderAdminKey.isBlank() || !orderAdminKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
    }
}
