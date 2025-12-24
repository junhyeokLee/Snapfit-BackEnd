// src/main/java/com/snapfit/snapfitbackend/controller/AlbumController.java
package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.album.dto.response.AlbumDetailResponse;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumPageEntity;
import com.snapfit.snapfitbackend.domain.album.dto.response.AlbumPageResponse;
import com.snapfit.snapfitbackend.domain.album.dto.request.CreateAlbumRequest;
import com.snapfit.snapfitbackend.domain.album.dto.response.CreateAlbumResponse;
import com.snapfit.snapfitbackend.domain.album.dto.request.SaveAlbumPageRequest;
import com.snapfit.snapfitbackend.domain.album.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Album", description = "앨범 및 페이지 관리 API")
@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    /**
     * 앨범 생성
     * - Flutter 커버 편집 결과를 그대로 넘겨서 생성
     *   ratio             : 커버/페이지 비율 (예: "3:4", "1:1")
     *   coverLayersJson   : 커버 LayerModel[] JSON
     *   coverImageUrl     : 커버 렌더링 이미지 URL
     *   albumThumbnailUrl : 앨범 목록용 썸네일 URL (없으면 커버와 동일하게 줄 수 있음)
     */
    @Operation(summary = "앨범 생성")
    @PostMapping
    public ResponseEntity<CreateAlbumResponse> createAlbum(
            @RequestBody CreateAlbumRequest request
    ) {
        // 서비스 호출: title 제거, 레이어 JSON/URL 기반으로 생성
        AlbumEntity album = albumService.createAlbum(
                request.getRatio(),
                request.getCoverLayersJson(),
                request.getCoverImageUrl(),
                request.getCoverThumbnailUrl()
        );

        // 엔티티 -> 응답 DTO 변환
        CreateAlbumResponse response = CreateAlbumResponse.builder()
                .albumId(album.getId())
                .ratio(album.getRatio())
                .coverLayersJson(album.getCoverLayersJson())
                .coverImageUrl(album.getCoverImageUrl())
                .coverThumbnailUrl(album.getCoverThumbnailUrl())
                .createdAt(album.getCreatedAt())
                .updatedAt(album.getUpdatedAt())
                .build();

        // 생성이므로 201 코드 사용
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 앨범의 페이지 리스트 조회
     */
    @Operation(summary = "앨범 페이지 목록 조회")
    @GetMapping("/{albumId}/pages")
    public ResponseEntity<List<AlbumPageResponse>> getAlbumPages(
            @PathVariable Long albumId
    ) {
        List<AlbumPageEntity> pages = albumService.getAlbumPages(albumId);

        List<AlbumPageResponse> response = pages.stream()
                .map(page -> AlbumPageResponse.builder()
                        .pageId(page.getId())
                        .albumId(page.getAlbum().getId())
                        .pageNumber(page.getPageNumber())
                        .layersJson(page.getLayersJson())
                        .imageUrl(page.getImageUrl())
                        .thumbnailUrl(page.getThumbnailUrl())
                        .build()
                )
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 페이지 저장/업데이트
     */
    @Operation(summary = "앨범 페이지 저장(업서트)")
    @PostMapping("/{albumId}/pages")
    public ResponseEntity<AlbumPageResponse> savePage(
            @PathVariable Long albumId,
            @RequestBody SaveAlbumPageRequest request
    ) {
        AlbumPageEntity page = albumService.savePage(
                albumId,
                request.getPageNumber(),
                request.getLayersJson(),
                request.getImageUrl(),
                request.getThumbnailUrl()
        );

        AlbumPageResponse response = AlbumPageResponse.builder()
                .pageId(page.getId())
                .albumId(page.getAlbum().getId())
                .pageNumber(page.getPageNumber())
                .layersJson(page.getLayersJson())
                .imageUrl(page.getImageUrl())
                .thumbnailUrl(page.getThumbnailUrl())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 상세 조회 (앨범 + 페이지 목록)
     */
    @Operation(summary = "앨범 상세 조회")
    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumDetailResponse> getAlbumDetail(
            @PathVariable Long albumId
    ) {
        // 1) 앨범, 페이지 조회
        AlbumEntity album = albumService.getAlbum(albumId);
        List<AlbumPageEntity> pages = albumService.getAlbumPages(albumId);

        // 2) 페이지 요약 DTO로 매핑
        List<AlbumDetailResponse.AlbumPageSummaryResponse> pageResponses = pages.stream()
                .map(page -> AlbumDetailResponse.AlbumPageSummaryResponse.builder()
                        .pageId(page.getId())
                        .pageNumber(page.getPageNumber())
                        .imageUrl(page.getImageUrl())
                        .thumbnailUrl(page.getThumbnailUrl())
                        .build()
                )
                .collect(Collectors.toList());

        // 3) 앨범 상세 DTO 구성
        AlbumDetailResponse response = AlbumDetailResponse.builder()
                .albumId(album.getId())
                .ratio(album.getRatio())
                .coverLayersJson(album.getCoverLayersJson())
                .coverImageUrl(album.getCoverImageUrl())
                .albumThumbnailUrl(album.getCoverThumbnailUrl())
                .totalPages(album.getTotalPages())
                .createdAt(album.getCreatedAt())
                .updatedAt(album.getUpdatedAt())
                .pages(pageResponses)
                .build();

        return ResponseEntity.ok(response);
    }
}