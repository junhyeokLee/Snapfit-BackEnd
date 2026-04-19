package com.snapfit.snapfitbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtSecretGuard implements ApplicationRunner {

    private static final String LEGACY_DEFAULT_SECRET =
            "DefaultsToARandomLongSecretKeyForDevelopmentPurposesOnly123456";

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public void run(ApplicationArguments args) {
        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
        if (!isProd) return;

        String secret = jwtSecret == null ? "" : jwtSecret.trim();
        if (secret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET is required in prod profile.");
        }
        if (LEGACY_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT_SECRET must not use legacy default value in prod.");
        }
        if (secret.length() < 64) {
            throw new IllegalStateException("JWT_SECRET must be at least 64 characters in prod.");
        }
    }
}
