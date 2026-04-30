package com.duke.auth.security;

import com.duke.auth.config.properties.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 令牌的生成、解析与校验。
 * 注销令牌通过 Redis 黑名单实现：将 token 字符串作为 key 写入 Redis，
 * TTL 设为剩余有效期，到期自动清除，无需维护永久集合。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    private SecretKey signingKey;

    /** 在所有依赖注入完成后，将字符串密钥转换为 HMAC 签名 Key */
    @PostConstruct
    private void init() {
        signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * 生成 JWT，subject 存用户名，claim 存 userId，
     * 方便下游直接从 Token 中取 userId，无需再查库。
     */
    public String generateToken(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration() * 1000);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 将 token 加入黑名单，TTL = token 剩余有效期。
     * 调用时机：主动注销、修改密码后使旧 token 失效。
     */
    public void invalidateToken(String token) {
        try {
            Date expiry = parseClaims(token).getExpiration();
            long ttlMs = expiry.getTime() - System.currentTimeMillis();
            if (ttlMs > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttlMs, TimeUnit.MILLISECONDS);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("注销时解析 token 失败，忽略: {}", e.getMessage());
        }
    }

    /**
     * 校验 token 签名和有效期，同时检查是否在 Redis 黑名单中。
     * 黑名单检查放在签名验证之后，避免对非法 token 发起 Redis 访问。
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token))) {
                log.warn("JWT 已注销（黑名单中）");
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
