package com.snapfit.snapfitbackend.domain.billing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BillingReadinessService {

    @Value("${snapfit.billing.mock-mode:true}")
    private boolean mockMode;

    @Value("${snapfit.billing.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Value("${snapfit.billing.toss.secret-key:}")
    private String tossSecretKey;

    @Value("${snapfit.billing.webhook.secret:}")
    private String webhookSecret;

    @Value("${snapfit.billing.webhook.toss.secret:}")
    private String tossWebhookSecret;

    @Value("${snapfit.billing.webhook.inicis.secret:}")
    private String inicisWebhookSecret;

    @Value("${snapfit.address.juso.enabled:true}")
    private boolean jusoEnabled;

    @Value("${snapfit.address.juso.key:}")
    private String jusoKey;

    public Map<String, Object> readiness() {
        List<Map<String, Object>> checks = new ArrayList<>();

        checks.add(check("mockModeOff", !mockMode, "SNAPFIT_BILLING_MOCK_MODE=false"));
        checks.add(check("publicBaseUrlHttps", publicBaseUrl != null && publicBaseUrl.startsWith("https://"),
                "SNAPFIT_BILLING_PUBLIC_BASE_URL should be HTTPS"));
        checks.add(check("tossSecretKey", hasText(tossSecretKey), "SNAPFIT_BILLING_TOSS_SECRET_KEY is set"));
        checks.add(check("webhookSecretToss", hasText(tossWebhookSecret) || hasText(webhookSecret),
                "Toss webhook secret is set (provider or global)"));
        checks.add(check("webhookSecretInicis", hasText(inicisWebhookSecret) || hasText(webhookSecret),
                "Inicis webhook secret is set (provider or global)"));

        if (jusoEnabled) {
            checks.add(check("jusoKey", hasText(jusoKey), "SNAPFIT_ADDRESS_JUSO_KEY is set"));
        } else {
            checks.add(check("jusoDisabled", true, "Address API disabled by config"));
        }

        long passed = checks.stream().filter(c -> Boolean.TRUE.equals(c.get("ok"))).count();
        boolean readyForLive = passed == checks.size();

        return Map.of(
                "readyForLive", readyForLive,
                "mode", mockMode ? "MOCK" : "LIVE",
                "checks", checks,
                "summary", Map.of(
                        "passed", passed,
                        "failed", checks.size() - passed,
                        "total", checks.size()
                )
        );
    }

    private Map<String, Object> check(String code, boolean ok, String message) {
        return Map.of(
                "code", code,
                "ok", ok,
                "message", message
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
