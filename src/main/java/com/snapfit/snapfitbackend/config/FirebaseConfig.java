package com.snapfit.snapfitbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URL;

/**
 * Firebase Admin SDK 초기화 설정.
 *
 * - prod 환경에서 Firebase Storage 기반 ImageStorageService 를 사용할 때 필요합니다.
 * - dev/local 에서는 NoopImageStorageService 를 사용하므로, 서비스 계정 키가 없어도 됩니다.
 */
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account.file:}")
    private String serviceAccountLocation;

    @Value("${firebase.storage.bucket:}")
    private String storageBucket;

    @PostConstruct
    public void init() throws Exception {
        // 설정이 비어 있으면 초기화 스킵 (예: dev/local)
        if (serviceAccountLocation == null || serviceAccountLocation.isBlank()
                || storageBucket == null || storageBucket.isBlank()) {
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return; // 이미 초기화 된 경우
        }

        try (InputStream serviceAccountStream = resolveServiceAccountStream(serviceAccountLocation)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .setStorageBucket(storageBucket)
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }

    private InputStream resolveServiceAccountStream(String location) throws Exception {
        if (location.startsWith("classpath:")) {
            String path = location.substring("classpath:".length());
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is == null) {
                throw new IllegalStateException("Cannot find service account file in classpath: " + path);
            }
            return is;
        } else if (location.startsWith("file:")) {
            String path = location.substring("file:".length());
            return new URL("file:" + path).openStream();
        } else {
            // 그냥 파일 경로로 취급
            return new URL("file:" + location).openStream();
        }
    }
}

