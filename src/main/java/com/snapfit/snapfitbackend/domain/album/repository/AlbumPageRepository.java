package com.snapfit.snapfitbackend.domain.album.repository;

import com.snapfit.snapfitbackend.domain.album.entity.AlbumPageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumPageRepository extends JpaRepository<AlbumPageEntity, Long> {

    // 특정 앨범의 모든 페이지
    List<AlbumPageEntity> findByAlbumId(Long albumId);

    // 특정 앨범 + 페이지 번호로 1개 조회 (업서트용)
    Optional<AlbumPageEntity> findByAlbumIdAndPageNumber(Long albumId, int pageNumber);

    // 이미지 URL 사용 여부 확인용
    boolean existsByOriginalUrl(String originalUrl);
    boolean existsByPreviewUrl(String previewUrl);
    boolean existsByImageUrl(String imageUrl);
    boolean existsByThumbnailUrl(String thumbnailUrl);
}