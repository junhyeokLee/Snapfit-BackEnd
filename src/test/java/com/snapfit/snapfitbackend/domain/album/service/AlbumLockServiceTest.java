package com.snapfit.snapfitbackend.domain.album.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class AlbumLockServiceTest {

    private InMemoryAlbumLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new InMemoryAlbumLockService();
    }

    @Test
    @DisplayName("한 사용자가 잠금을 획득하고 다른 사용자가 접근하면 실패해야 함")
    void testConcurrentLocking() {
        Long albumId = 1L;
        String userA = "user_A";
        String userB = "user_B";

        // User A가 잠금 획득
        assertDoesNotThrow(() -> lockService.lock(albumId, userA));
        assertEquals(userA, lockService.getLocker(albumId));

        // User B가 동시에 접근 -> 실패(Exception) 해야 함
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            lockService.lock(albumId, userB);
        });

        assertTrue(exception.getMessage().contains("ALREADY_LOCKED_BY_user_A"));
    }

    @Test
    @DisplayName("잠금 해제 후에는 다른 사용자가 잠금을 획득할 수 있어야 함")
    void testUnlockAndReLock() {
        Long albumId = 1L;
        String userA = "user_A";
        String userB = "user_B";

        lockService.lock(albumId, userA);
        lockService.unlock(albumId, userA);

        // 이제 User B가 획득 가능해야 함
        assertDoesNotThrow(() -> lockService.lock(albumId, userB));
        assertEquals(userB, lockService.getLocker(albumId));
    }

    @Test
    @DisplayName("자기 자신이 이미 잠근 경우 만료 시간만 갱신되어야 함")
    void testSelfLockRenewal() {
        Long albumId = 1L;
        String userA = "user_A";

        assertDoesNotThrow(() -> lockService.lock(albumId, userA));
        assertDoesNotThrow(() -> lockService.lock(albumId, userA)); // 중복 호출 허용
    }
}
