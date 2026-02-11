package com.snapfit.snapfitbackend.domain.image;

import com.google.cloud.storage.Blob;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * prod 환경에서 실제 Firebase Storage 에서 이미지를 삭제하는 구현체.
 *
 * URL 은 보통 다음 두 형태 중 하나일 수 있습니다.
 * -
 * https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{path}%2Ffile.jpg?alt=media
 * - gs://{bucket}/{path}/file.jpg
 *
 * DB 에 저장하는 URL 규칙에 맞춰 extractObjectNameFromUrl() 로 object name 을 추출한 뒤 삭제합니다.
 */
@Slf4j
@Service
@Profile("prod")
public class FirebaseImageStorageService implements ImageStorageService {

    @org.springframework.beans.factory.annotation.Value("${firebase.storage.bucket}")
    private String bucketName;

    @Override
    public String upload(org.springframework.web.multipart.MultipartFile file, String directory) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = directory + "/" + java.util.UUID.randomUUID() + extension;

            StorageClient.getInstance().bucket(bucketName).create(
                    filename,
                    file.getInputStream(),
                    file.getContentType());

            // Firebase Storage 다운로드 URL 생성 (공개 읽기 권한 필요할 수 있음, or Signed URL)
            // 여기서는 기본 미디어 다운로드 URL 패턴 사용
            // https://firebasestorage.googleapis.com/v0/b/<bucket>/o/<path>?alt=media
            return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    bucketName,
                    java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to upload image to Firebase Storage", e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        try {
            String objectName = extractObjectNameFromUrl(url);
            if (objectName == null || objectName.isBlank()) {
                log.warn("Cannot extract object name from url: {}", url);
                return;
            }

            Blob blob = StorageClient.getInstance().bucket().get(objectName);
            if (blob == null) {
                log.info("Blob not found for url={}, objectName={}", url, objectName);
                return;
            }

            boolean deleted = blob.delete();
            log.info("Firebase Storage delete result. url={}, objectName={}, deleted={}", url, objectName, deleted);
        } catch (Exception e) {
            // 운영 환경에서는 실패 로그만 남기고 서비스 로직은 계속 진행
            log.error("Failed to delete image from Firebase Storage. url={}", url, e);
        }
    }

    /**
     * Firebase Storage 다운로드 URL 또는 gs:// URL 에서 object name(path)를 추출.
     * 실제 URL 형식에 맞춰 이 로직은 필요하면 조정하면 됩니다.
     */
    private String extractObjectNameFromUrl(String url) {
        try {
            if (url.startsWith("gs://")) {
                // 형식: gs://bucket/path/to/file.jpg
                String withoutScheme = url.substring("gs://".length());
                int firstSlash = withoutScheme.indexOf('/');
                if (firstSlash < 0) {
                    return null;
                }
                return withoutScheme.substring(firstSlash + 1);
            }

            // 예시:
            // https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{path}%2Ffile.jpg?alt=media
            int index = url.indexOf("/o/");
            if (index < 0) {
                return null;
            }
            String afterO = url.substring(index + 3); // "xxx%2Fyyy.jpg?alt=media"
            int queryIdx = afterO.indexOf("?");
            String encodedPath = (queryIdx > 0) ? afterO.substring(0, queryIdx) : afterO;

            // %2F -> '/'
            return java.net.URLDecoder.decode(encodedPath, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to parse object name from url={}", url, e);
            return null;
        }
    }
}
