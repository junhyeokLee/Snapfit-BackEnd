package com.snapfit.snapfitbackend.network;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtProvider {
    @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
    private String secretKey;

    private java.security.Key key;

    @javax.annotation.PostConstruct
    protected void init() {
        // Base64 encoded key or just string bytes depending on preference.
        // For HS256, key length should be >= 256 bits (32 chars).
        // If the secret is short, we can pad or hash it, but better to enforce strong
        // key.
        byte[] keyBytes = java.nio.charset.StandardCharsets.UTF_8.encode(secretKey).array();
        // If provided key is too weak, Keys.hmacShaKeyFor might complain or weak
        // security.
        // For simplicity in this demo transition, we use the provided string directly
        // if long enough,
        // or fall back to safe generation if missing (but log warning).
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    private final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 60; // 1시간
    private final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 14; // 14일

    public String createAccessToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String getUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getRefreshTokenExpiration() {
        return REFRESH_TOKEN_EXPIRATION;
    }
}