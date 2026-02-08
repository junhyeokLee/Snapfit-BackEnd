package com.snapfit.snapfitbackend.domain.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 기본/로컬 환경용 No-Op 구현체.
 * - 실제로는 아무 것도 지우지 않고 로그만 남깁니다.
 * - prod 환경에서는 Firebase/S3 등 실제 구현체로 교체하면 됩니다.
 */
@Slf4j
@Service
@Profile({"dev", "default"})
public class NoopImageStorageService implements ImageStorageService {

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        log.info("[NoopImageStorageService] delete called for url={}", url);
    }
}

