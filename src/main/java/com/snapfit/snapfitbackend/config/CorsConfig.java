package com.snapfit.snapfitbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS(Cross-Origin Resource Sharing) 설정.
 *
 * 프론트엔드(Flutter 웹/모바일, React 등)가 다른 도메인/포트에서 백엔드 API를 호출할 수 있도록 허용합니다.
 *
 * 운영 환경에서는 CORS_ALLOWED_ORIGINS 환경 변수로 허용할 도메인을 제한하는 것을 권장합니다.
 * 예: CORS_ALLOWED_ORIGINS=https://app.snapfit.com,https://www.snapfit.com
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);

        // 환경 변수로 허용할 origin 설정 (운영 환경에서 사용)
        if (allowedOrigins != null && !allowedOrigins.equals("*") && !allowedOrigins.isBlank()) {
            String[] origins = allowedOrigins.split(",");
            for (String origin : origins) {
                config.addAllowedOrigin(origin.trim());
            }
        } else {
            // 개발 환경: 모든 origin 허용
            config.addAllowedOriginPattern("*");
        }

        config.addAllowedHeader("*"); // 모든 헤더 허용
        config.addAllowedMethod("*"); // 모든 HTTP 메서드 허용 (GET, POST, PUT, DELETE 등)

        // CORS preflight 요청 캐시 시간 (초)
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
