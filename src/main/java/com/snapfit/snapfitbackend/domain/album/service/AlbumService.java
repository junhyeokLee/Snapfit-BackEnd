package com.snapfit.snapfitbackend.domain.album.service;

import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumPageEntity;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumPageRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumPageRepository albumPageRepository;

    /**
     * 앨범 생성
     * - 제목은 사용하지 않고, 비율 + 커버 레이어 JSON + 커버/썸네일 URL만 저장
     * - createdAt/updatedAt, totalPages 는 엔티티 @PrePersist 에서 처리
     */
    @Transactional
    public AlbumEntity createAlbum(
            String ratio,
            String coverLayersJson,
            String coverImageUrl,
            String coverThumbnailUrl
    ) {
        // 엔티티 빌더로 앨범 생성
        AlbumEntity album = AlbumEntity.builder()
                .ratio(ratio)
                .coverLayersJson(coverLayersJson)      // 커버 레이어 JSON
                .coverImageUrl(coverImageUrl)          // 커버 최종 렌더 이미지
                .coverThumbnailUrl(coverThumbnailUrl)  // 목록용 썸네일 (없으면 null 허용)
                .build();

        return albumRepository.save(album);
    }

    /**
     * 앨범의 모든 페이지 조회
     */
    @Transactional(readOnly = true)
    public List<AlbumPageEntity> getAlbumPages(Long albumId) {
        return albumPageRepository.findByAlbumId(albumId);
    }

    /**
     * 페이지 저장 (업서트: 있으면 업데이트, 없으면 새로 생성)
     *
     * @param albumId        앨범 ID
     * @param pageNumber     페이지 번호 (1, 2, 3, ...)
     * @param layersJson     Flutter LayerModel[] JSON 직렬화 값
     * @param renderImageUrl 인쇄/미리보기용 원본 이미지 URL
     * @param thumbnailUrl   썸네일 이미지 URL (없으면 null 가능)
     */
    @Transactional
    public AlbumPageEntity savePage(
            Long albumId,
            int pageNumber,
            String layersJson,
            String renderImageUrl,
            String thumbnailUrl
    ) {
        // 1) 앨범 엔티티 조회 (없으면 예외)
        AlbumEntity album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범을 찾을 수 없습니다. id=" + albumId));

        // 2) 기존 페이지 있는지 확인 (없으면 새 엔티티)
        AlbumPageEntity page = albumPageRepository
                .findByAlbumIdAndPageNumber(albumId, pageNumber)
                .orElseGet(() -> AlbumPageEntity.builder()
                        .album(album)
                        .pageNumber(pageNumber)
                        .build()
                );

        // 3) 내용 업데이트
        page.setLayersJson(layersJson);
        page.setImageUrl(renderImageUrl);
        page.setThumbnailUrl(thumbnailUrl);

        // 4) 저장
        return albumPageRepository.save(page);
    }

    /**
     * 앨범 단건 조회
     */
    @Transactional(readOnly = true)
    public AlbumEntity getAlbum(Long albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범을 찾을 수 없습니다. id=" + albumId));
    }
}