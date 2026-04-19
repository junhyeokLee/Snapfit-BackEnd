package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.ops.service.OpsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsController {

    private final OpsDashboardService opsDashboardService;

    @Value("${snapfit.order.admin-key:}")
    private String orderAdminKey;

    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        ensureOrderAdmin(adminKey);
        return ResponseEntity.ok(opsDashboardService.dashboard());
    }

    @GetMapping("/admin/cs-signals")
    public ResponseEntity<Map<String, Object>> csSignals(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestParam(defaultValue = "50") int limit
    ) {
        ensureOrderAdmin(adminKey);
        return ResponseEntity.ok(opsDashboardService.csSignals(limit));
    }

    private void ensureOrderAdmin(String key) {
        if (orderAdminKey == null || orderAdminKey.isBlank() || !orderAdminKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
    }
}
