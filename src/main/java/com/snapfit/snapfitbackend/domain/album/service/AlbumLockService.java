package com.snapfit.snapfitbackend.domain.album.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 앨범 편집 잠금 서비스 인터페이스
 */
public interface AlbumLockService {
    void lock(Long albumId, String userId);

    void unlock(Long albumId, String userId);

    String getLocker(Long albumId);
}

/**
 * Redis 기반 실 운영용 락 서비스
 * application.yml에 snapfit.lock.type=redis 설정이 있어야 활성화됨
 */
@Service
@ConditionalOnProperty(name = "snapfit.lock.type", havingValue = "redis")
@RequiredArgsConstructor
class RedisAlbumLockService implements AlbumLockService {
    private final StringRedisTemplate redisTemplate;
    private static final long LOCK_TIMEOUT_MINUTES = 5;

    @Override
    public void lock(Long albumId, String userId) {
        try {
            String key = "lock:album:" + albumId;
            String currentLocker = redisTemplate.opsForValue().get(key);

            if (currentLocker != null) {
                if (currentLocker.equals(userId)) {
                    redisTemplate.expire(key, Duration.ofMinutes(LOCK_TIMEOUT_MINUTES));
                    return;
                }
                throw new IllegalStateException("ALREADY_LOCKED_BY_" + currentLocker);
            }

            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    userId,
                    Duration.ofMinutes(LOCK_TIMEOUT_MINUTES));

            if (Boolean.FALSE.equals(success)) {
                String locker = redisTemplate.opsForValue().get(key);
                throw new IllegalStateException("ALREADY_LOCKED_BY_" + (locker != null ? locker : "UNKNOWN"));
            }
        } catch (IllegalStateException e) {
            throw e; // 이미 잠긴 경우는 의도된 예외이므로 그대로 던짐
        } catch (Exception e) {
            // Redis 연결 실패 등 인프라 에러 시, 편집 진입을 차단하지 않도록 로그만 남기고 통과 (Fail-Safe)
            System.err.println("[RedisLock] Connection failed, bypassing lock: " + e.getMessage());
        }
    }

    @Override
    public void unlock(Long albumId, String userId) {
        try {
            String key = "lock:album:" + albumId;
            String currentLocker = redisTemplate.opsForValue().get(key);
            if (currentLocker != null && currentLocker.equals(userId)) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            System.err.println("[RedisLock] Unlock failed: " + e.getMessage());
        }
    }

    @Override
    public String getLocker(Long albumId) {
        try {
            String key = "lock:album:" + albumId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            System.err.println("[RedisLock] GetLocker failed: " + e.getMessage());
            return null;
        }
    }
}

/**
 * 인메모리 락 매니저 (Redis 없이 작동)
 * 기본값이거나 snapfit.lock.type=in-memory 일 때 활성화
 */
@Service
@ConditionalOnProperty(name = "snapfit.lock.type", havingValue = "in-memory", matchIfMissing = true)
@RequiredArgsConstructor
class InMemoryAlbumLockService implements AlbumLockService {
    private final ConcurrentHashMap<Long, LockInfo> locks = new ConcurrentHashMap<>();
    private static final long LOCK_TIMEOUT_MINUTES = 5;

    @Data
    @AllArgsConstructor
    private static class LockInfo {
        String userId;
        long expiryTime;
    }

    @Override
    public void lock(Long albumId, String userId) {
        long now = System.currentTimeMillis();
        LockInfo info = locks.get(albumId);

        if (info != null && info.expiryTime > now) {
            if (info.userId.equals(userId)) {
                info.expiryTime = now + TimeUnit.MINUTES.toMillis(LOCK_TIMEOUT_MINUTES);
                return;
            }
            throw new IllegalStateException("ALREADY_LOCKED_BY_" + info.userId);
        }

        locks.put(albumId, new LockInfo(userId, now + TimeUnit.MINUTES.toMillis(LOCK_TIMEOUT_MINUTES)));
    }

    @Override
    public void unlock(Long albumId, String userId) {
        LockInfo info = locks.get(albumId);
        if (info != null && info.userId.equals(userId)) {
            locks.remove(albumId);
        }
    }

    @Override
    public String getLocker(Long albumId) {
        long now = System.currentTimeMillis();
        LockInfo info = locks.get(albumId);
        if (info != null && info.expiryTime > now) {
            return info.userId;
        }
        if (info != null) {
            locks.remove(albumId);
        }
        return null;
    }
}
