package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.order.dto.OrderQuoteResponse;
import com.snapfit.snapfitbackend.domain.order.dto.OrderResponse;
import com.snapfit.snapfitbackend.domain.order.dto.OrderPageResponse;
import com.snapfit.snapfitbackend.domain.order.dto.OrderSummaryResponse;
import com.snapfit.snapfitbackend.domain.order.dto.PrintPackageResponse;
import com.snapfit.snapfitbackend.domain.order.entity.OrderStatus;
import com.snapfit.snapfitbackend.domain.order.service.AddressSearchService;
import com.snapfit.snapfitbackend.domain.order.service.OrderService;
import com.snapfit.snapfitbackend.global.security.InMemoryRequestGuard;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final AddressSearchService addressSearchService;
    private final InMemoryRequestGuard requestGuard;
    private final Environment environment;

    @Value("${snapfit.order.admin-key:}")
    private String orderAdminKey;

    @Value("${snapfit.push.admin-key:}")
    private String pushAdminKey;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> list(@RequestParam String userId) {
        return ResponseEntity.ok(orderService.listByUser(userId));
    }

    @GetMapping("/paged")
    public ResponseEntity<OrderPageResponse> listPaged(
            @RequestParam String userId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(orderService.listByUserPaged(userId, status, page, size));
    }

    @GetMapping("/summary")
    public ResponseEntity<OrderSummaryResponse> summary(@RequestParam String userId) {
        return ResponseEntity.ok(orderService.summarizeByUser(userId));
    }

    @GetMapping("/admin/paged")
    public ResponseEntity<OrderPageResponse> listAdminPaged(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ensureOrderAdmin(key);
        return ResponseEntity.ok(orderService.listAdminPaged(status, keyword, page, size));
    }

    @GetMapping("/admin/summary")
    public ResponseEntity<?> adminSummary(
            @RequestHeader(value = "X-Admin-Key", required = false) String key
    ) {
        ensureOrderAdmin(key);
        return ResponseEntity.ok(orderService.summarizeAdmin());
    }

    @PostMapping("/admin/{orderId}/print-package/prepare")
    public ResponseEntity<OrderResponse> preparePrintPackage(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key
    ) {
        ensureOrderAdmin(key);
        return ResponseEntity.ok(orderService.preparePrintPackage(orderId));
    }

    @GetMapping("/admin/{orderId}/print-package")
    public ResponseEntity<PrintPackageResponse> getPrintPackage(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key
    ) {
        ensureOrderAdmin(key);
        return ResponseEntity.ok(orderService.getPrintPackage(orderId));
    }

    @GetMapping(value = "/admin/{orderId}/print-package.zip", produces = "application/zip")
    public ResponseEntity<byte[]> downloadPrintPackageZip(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key
    ) {
        ensureOrderAdmin(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentName(orderId, "print-package.zip"))
                .body(orderService.exportPrintPackageZip(orderId));
    }

    @GetMapping(value = "/admin/{orderId}/print-package.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPrintPackagePdf(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key
    ) {
        ensureOrderAdmin(key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentName(orderId, "print-package.pdf"))
                .body(orderService.exportPrintPackagePdf(orderId));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            HttpServletRequest http
    ) {
        String userId = request.userId == null ? "unknown" : request.userId.trim();
        String key = "order:create:" + clientIp(http) + ":" + userId;
        if (!requestGuard.allowRate(key, 10, 60)) {
            log.warn("rate-limit create-order blocked ip={} userId={}", clientIp(http), userId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many create-order requests");
        }
        log.info(
                "order-create request ip={} userId={} albumId={} paymentMethod={} pageCount={}",
                clientIp(http),
                userId,
                request.albumId,
                request.paymentMethod,
                request.pageCount
        );
        return ResponseEntity.ok(orderService.createPrintOrder(
                request.userId,
                request.albumId,
                request.title,
                request.amount,
                request.pageCount,
                request.paymentMethod,
                request.recipientName,
                request.recipientPhone,
                request.zipCode,
                request.addressLine1,
                request.addressLine2,
                request.deliveryMemo));
    }

    @GetMapping("/quote")
    public ResponseEntity<OrderQuoteResponse> quote(
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) Integer pageCount
    ) {
        return ResponseEntity.ok(orderService.quote(albumId, pageCount));
    }

    @GetMapping("/address/search")
    public ResponseEntity<?> searchAddress(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            HttpServletRequest http
    ) {
        String key = "order:addr-search:" + clientIp(http);
        if (!requestGuard.allowRate(key, 60, 60)) {
            log.warn("rate-limit address-search blocked ip={} page={}", clientIp(http), page);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many address-search requests");
        }
        try {
            return ResponseEntity.ok(addressSearchService.search(keyword, page));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    @GetMapping(value = "/{orderId}/payment/checkout", produces = MediaType.TEXT_HTML_VALUE)
    public String paymentCheckout(
            @PathVariable String orderId,
            @RequestParam(required = false) String provider
    ) {
        String safeProvider = provider == null || provider.isBlank() ? "TOSS_PAYMENTS" : provider;
        String successDeeplink = "snapfit://order/success?orderId=" + enc(orderId) + "&provider=" + enc(safeProvider);
        String failDeeplink = "snapfit://order/fail?orderId=" + enc(orderId) + "&provider=" + enc(safeProvider);
        return """
                <!DOCTYPE html>
                <html lang=\"ko\">
                <head><meta charset=\"UTF-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                  <title>주문 결제</title>
                  <style>
                    body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;padding:24px;background:#f4f8fb}
                    .card{max-width:480px;margin:0 auto;background:#fff;border-radius:14px;padding:18px;box-shadow:0 8px 24px rgba(0,0,0,.08)}
                    .meta{font-size:14px;color:#54606d;margin:6px 0 16px}
                    button{width:100%%;padding:12px;border:0;border-radius:10px;font-weight:700;cursor:pointer}
                    .ok{background:#07b8de;color:#fff}
                    .fail{background:#e55c5c;color:#fff;margin-top:8px}
                  </style>
                </head>
                <body>
                <div class=\"card\">
                  <h2>주문 결제</h2>
                  <p class=\"meta\">주문번호: %s<br/>결제수단: %s</p>
                  <button class=\"ok\" onclick=\"ok()\">결제 완료</button>
                  <button class=\"fail\" onclick=\"fail()\">결제 실패/취소</button>
                </div>
                <script>
                  const okUrl = '%s';
                  const failUrl = '%s';
                  function ok(){ window.location.href = okUrl; }
                  function fail(){ window.location.href = failUrl; }
                </script>
                </body>
                </html>
                """.formatted(orderId, safeProvider, successDeeplink, failDeeplink);
    }

    @PostMapping("/{orderId}/payment/confirm")
    public ResponseEntity<OrderResponse> confirmPayment(
            @PathVariable String orderId,
            HttpServletRequest http
    ) {
        String key = "order:confirm:" + clientIp(http) + ":" + orderId;
        if (!requestGuard.allowRate(key, 20, 60)) {
            log.warn("rate-limit confirm-payment blocked ip={} orderId={}", clientIp(http), orderId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many confirm-payment requests");
        }
        log.info("order-confirm request ip={} orderId={}", clientIp(http), orderId);
        return ResponseEntity.ok(orderService.confirmPaymentAndSubmitPrint(orderId));
    }

    @PostMapping("/{orderId}/shipping")
    public ResponseEntity<?> markShipping(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestBody MarkShippingRequest request
    ) {
        ensureOrderAdmin(key);
        return ResponseEntity.ok(orderService.markShipped(orderId, request.courier, request.trackingNumber));
    }

    @PostMapping("/{orderId}/delivered")
    public ResponseEntity<?> markDelivered(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key
    ) {
        ensureOrderAdmin(key);
        return ResponseEntity.ok(orderService.markDelivered(orderId));
    }

    @PostMapping("/test/create")
    public ResponseEntity<OrderResponse> createTestOrder(@RequestBody CreateTestOrderRequest request) {
        ensureNonProdTestApi();
        return ResponseEntity.ok(orderService.createTestOrder(request.userId, request.title, request.amount));
    }

    @PostMapping("/{orderId}/advance")
    public ResponseEntity<OrderResponse> advance(@PathVariable String orderId) {
        ensureNonProdTestApi();
        return ResponseEntity.ok(orderService.advanceStatus(orderId));
    }

    @PostMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> setStatus(
            @PathVariable String orderId,
            @RequestBody SetOrderStatusRequest request
    ) {
        ensureNonProdTestApi();
        OrderStatus status = OrderStatus.valueOf(request.status);
        return ResponseEntity.ok(orderService.setStatus(orderId, status));
    }

    @GetMapping("/status-options")
    public ResponseEntity<?> statusOptions() {
        ensureNonProdTestApi();
        return ResponseEntity.ok(Map.of(
                "statuses", List.of(
                        OrderStatus.PAYMENT_PENDING.name(),
                        OrderStatus.PAYMENT_COMPLETED.name(),
                        OrderStatus.IN_PRODUCTION.name(),
                        OrderStatus.SHIPPING.name(),
                        OrderStatus.DELIVERED.name(),
                        OrderStatus.CANCELED.name())));
    }

    @Data
    public static class CreateTestOrderRequest {
        public String userId;
        public String title;
        public int amount;
    }

    @Data
    public static class CreateOrderRequest {
        public String userId;
        public Long albumId;
        public String title;
        public int amount;
        public Integer pageCount;
        public String paymentMethod;
        public String recipientName;
        public String recipientPhone;
        public String zipCode;
        public String addressLine1;
        public String addressLine2;
        public String deliveryMemo;
    }

    @Data
    public static class MarkShippingRequest {
        public String courier;
        public String trackingNumber;
    }

    @Data
    public static class SetOrderStatusRequest {
        public String status;
    }

    private void ensureNonProdTestApi() {
        if (environment.matchesProfiles("prod")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void ensureOrderAdmin(String key) {
        String effectiveAdminKey = (orderAdminKey != null && !orderAdminKey.isBlank())
                ? orderAdminKey
                : pushAdminKey;
        if (effectiveAdminKey == null || effectiveAdminKey.isBlank() || !effectiveAdminKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String attachmentName(String orderId, String suffix) {
        String safeOrderId = orderId == null ? "order" : orderId.replaceAll("[^A-Za-z0-9._-]", "_");
        return "attachment; filename=\"" + safeOrderId + "-" + suffix + "\"";
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
}
