package com.snapfit.snapfitbackend.network;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String path = request.getRequestURI();
        log.info("Processing request: {} {}", request.getMethod(), path);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                log.info("Token found, validating...");
                if (jwtProvider.validateToken(token)) {
                    String userId = jwtProvider.getUserId(token);
                    log.info("Valid Token for path: {}, UserId: {}", path, userId);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null,
                            Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.warn("Invalid Token for path: {} (validateToken returned false)", path);
                }
            } catch (Exception e) {
                log.error("Token validation error for path: {}", path, e);
            }
        } else {
            log.debug("No Auth Header or not Bearer for path: {}", path);
            if (authHeader != null) {
                log.debug("Auth Header present but invalid format: {}", authHeader);
            }
        }
        filterChain.doFilter(request, response);
    }
}