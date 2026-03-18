package com.snapfit.snapfitbackend.domain.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 로컬/개발 환경에서 파일 시스템에 이미지를 저장하는 구현체.
 */
@Slf4j
@Service
@Primary
@Profile("!prod")
public class FileSystemImageStorageService implements ImageStorageService {

    @Value("${snapfit.storage.local-dir:/tmp/snapfit-uploads}")
    private String localStorageDir;

    @Override
    public String upload(MultipartFile file, String directory) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        try {
            File dir = new File(localStorageDir + File.separator + directory);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("Failed to create local upload directory: " + dir.getAbsolutePath());
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;
            Path path = Paths.get(localStorageDir, directory, filename);
            Files.write(path, file.getBytes());

            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            return baseUrl + "/images/" + directory + "/" + filename;

        } catch (IOException e) {
            log.error("Failed to store file directly", e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public void delete(String url) {
        // 로컬 개발 환경에서는 굳이 삭제까지 안 해도 무방하거나,
        // URL 파싱해서 로컬 파일 삭제 구현 가능
        log.info("Mock delete image from local storage: {}", url);
    }
}
