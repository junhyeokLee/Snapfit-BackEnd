package com.snapfit.snapfitbackend.global.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryRequestGuard {

    private static final class CounterWindow {
        final long windowStartEpochSec;
        final AtomicInteger count = new AtomicInteger(0);

        CounterWindow(long windowStartEpochSec) {
            this.windowStartEpochSec = windowStartEpochSec;
        }
    }

    private final Map<String, CounterWindow> rateWindows = new ConcurrentHashMap<>();
    private final Map<String, Long> replayKeys = new ConcurrentHashMap<>();

    public boolean allowRate(String key, int maxRequests, int windowSeconds) {
        final int safeMax = Math.max(1, maxRequests);
        final int safeWindow = Math.max(1, windowSeconds);
        final long now = Instant.now().getEpochSecond();
        final long slot = now / safeWindow;
        final String slotKey = key + ":" + slot;

        CounterWindow counter = rateWindows.computeIfAbsent(slotKey, k -> new CounterWindow(now));
        int current = counter.count.incrementAndGet();
        cleanupRate(now, safeWindow * 3L);
        return current <= safeMax;
    }

    public boolean tryRegisterReplayKey(String key, int ttlSeconds) {
        final int safeTtl = Math.max(10, ttlSeconds);
        final long now = Instant.now().getEpochSecond();
        final long expiresAt = now + safeTtl;
        Long previous = replayKeys.putIfAbsent(key, expiresAt);
        cleanupReplay(now);
        return previous == null;
    }

    private void cleanupRate(long nowEpochSec, long keepSeconds) {
        if (rateWindows.size() < 2000) return;
        rateWindows.entrySet().removeIf(entry ->
                entry.getValue().windowStartEpochSec < nowEpochSec - keepSeconds);
    }

    private void cleanupReplay(long nowEpochSec) {
        if (replayKeys.size() < 2000) return;
        replayKeys.entrySet().removeIf(entry -> entry.getValue() < nowEpochSec);
    }
}

