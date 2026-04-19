package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.ops.service.OpsDashboardService;
import com.snapfit.snapfitbackend.domain.order.dto.OrderPageResponse;
import com.snapfit.snapfitbackend.domain.order.dto.OrderResponse;
import com.snapfit.snapfitbackend.domain.order.service.OrderService;
import com.snapfit.snapfitbackend.domain.support.service.SupportInquiryService;
import com.snapfit.snapfitbackend.domain.template.service.TemplateAiDraftService;
import com.snapfit.snapfitbackend.domain.template.service.TemplateService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminWebController {
    private static final String FIXED_ADMIN_KEY = "snapfit_admin_console_v1";

    private final OpsDashboardService opsDashboardService;
    private final OrderService orderService;
    private final TemplateService templateService;
    private final TemplateAiDraftService templateAiDraftService;
    private final SupportInquiryService supportInquiryService;

    @Value("${snapfit.order.admin-key:}")
    private String orderAdminKey;

    @Value("${snapfit.push.admin-key:}")
    private String pushAdminKey;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(opsDashboardService.dashboard());
    }

    @GetMapping("/cs-signals")
    public ResponseEntity<Map<String, Object>> csSignals(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestParam(defaultValue = "50") int limit
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(opsDashboardService.csSignals(limit));
    }

    @GetMapping("/orders/paged")
    public ResponseEntity<OrderPageResponse> listOrders(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(orderService.listAdminPaged(status, keyword, page, size));
    }

    @PostMapping("/orders/{orderId}/shipping")
    public ResponseEntity<OrderResponse> markShipping(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable String orderId,
            @RequestBody MarkShippingRequest request
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(orderService.markShipped(orderId, request.courier, request.trackingNumber));
    }

    @PostMapping("/orders/{orderId}/delivered")
    public ResponseEntity<OrderResponse> markDelivered(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable String orderId
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(orderService.markDelivered(orderId));
    }

    @PostMapping("/orders/{orderId}/print-package/prepare")
    public ResponseEntity<OrderResponse> preparePrintPackage(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable String orderId
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(orderService.preparePrintPackage(orderId));
    }

    @GetMapping(value = "/orders/{orderId}/print-package.zip", produces = "application/zip")
    public ResponseEntity<byte[]> downloadPrintPackageZip(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable String orderId
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentName(orderId, "print-package.zip"))
                .body(orderService.exportPrintPackageZip(orderId));
    }

    @GetMapping(value = "/orders/{orderId}/print-package.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPrintPackagePdf(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable String orderId
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentName(orderId, "print-package.pdf"))
                .body(orderService.exportPrintPackagePdf(orderId));
    }

    @GetMapping("/inquiries/paged")
    public ResponseEntity<?> inquiriesPaged(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(supportInquiryService.listAdmin(status, keyword, page, size));
    }

    @PostMapping("/inquiries/{id}/resolve")
    public ResponseEntity<?> resolveInquiry(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long id,
            @RequestBody ResolveInquiryRequest request
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(supportInquiryService.resolve(id, request.resolvedBy));
    }

    @GetMapping("/templates/paged")
    public ResponseEntity<?> templatesPaged(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(templateService.getAdminTemplatePage(page, size));
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<?> templateDetail(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long id
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(templateService.getAdminTemplateDetail(id));
    }

    @PostMapping("/templates/upsert")
    public ResponseEntity<?> upsertTemplate(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestBody TemplateController.TemplateUpsertRequest request
    ) {
        ensureAdmin(adminKey);
        var saved = templateService.upsertTemplateFromAdmin(request.toEntity());
        return ResponseEntity.ok(Map.of("id", saved.getId()));
    }

    @PostMapping("/templates/validate")
    public ResponseEntity<?> validateTemplateDraft(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestBody TemplateController.TemplateUpsertRequest request
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(templateService.validateTemplateDraftForAdmin(request.toEntity()));
    }

    @PostMapping("/templates/ai-draft")
    public ResponseEntity<?> generateTemplateAiDraft(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestBody AiDraftRequest request
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(templateAiDraftService.generateDraft(
                request.title,
                request.style,
                request.description,
                request.pageCount
        ));
    }

    @PostMapping("/templates/{id}/active")
    public ResponseEntity<?> setTemplateActive(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long id,
            @RequestBody ActiveToggleRequest request
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(templateService.setTemplateActive(id, request.active));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<?> deleteTemplate(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long id
    ) {
        ensureAdmin(adminKey);
        return ResponseEntity.ok(templateService.deleteTemplateForAdmin(id));
    }

    private void ensureAdmin(String key) {
        if (!Objects.equals(FIXED_ADMIN_KEY, key)
                && !Objects.equals(orderAdminKey, key)
                && !Objects.equals(pushAdminKey, key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
    }

    private String attachmentName(String orderId, String suffix) {
        String safeOrderId = orderId == null ? "order" : orderId.replaceAll("[^A-Za-z0-9._-]", "_");
        return "attachment; filename=\"" + safeOrderId + "-" + suffix + "\"";
    }

    @Data
    public static class MarkShippingRequest {
        public String courier;
        public String trackingNumber;
    }

    @Data
    public static class ActiveToggleRequest {
        public boolean active;
    }

    @Data
    public static class ResolveInquiryRequest {
        public String resolvedBy;
    }

    @Data
    public static class AiDraftRequest {
        public String title;
        public String style;
        public String description;
        public Integer pageCount;
    }
}
