package com.snapfit.snapfitbackend.config;

import com.snapfit.snapfitbackend.network.JwtAuthenticationFilter;
import com.snapfit.snapfitbackend.network.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtProvider jwtProvider;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints only: auth entrypoints, public assets/docs,
                                                // external callbacks/webhooks, and public invite/deeplink pages.
                                                .requestMatchers(
                                                                "/api/auth/login/**",
                                                                "/api/auth/refresh",
                                                                "/admin/**",
                                                                "/api/admin/**",
                                                                "/invite",
                                                                "/invite/preview.png",
                                                                "/api/invites/*",
                                                                "/api/billing/webhook/**",
                                                                "/api/billing/naverpay/webhook",
                                                                "/api/billing/return/**",
                                                                "/api/billing/mock/checkout",
                                                                "/api/orders/*/payment/checkout",
                                                                "/template-assets/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/templates/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
