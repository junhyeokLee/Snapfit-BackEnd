// src/main/java/com/snapfit/snapfitbackend/controller/AlbumController.java
package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.album.dto.request.InviteAlbumRequest;
import com.snapfit.snapfitbackend.domain.album.dto.response.AlbumDetailResponse;
import com.snapfit.snapfitbackend.domain.album.dto.response.AlbumListResponse;
import com.snapfit.snapfitbackend.domain.album.dto.response.AlbumMemberResponse;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberRole;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumPageEntity;
import com.snapfit.snapfitbackend.domain.album.dto.response.AlbumPageResponse;
import com.snapfit.snapfitbackend.domain.album.dto.request.CreateAlbumRequest;
import com.snapfit.snapfitbackend.domain.album.dto.response.CreateAlbumResponse;
import com.snapfit.snapfitbackend.domain.album.dto.request.SaveAlbumPageRequest;
import com.snapfit.snapfitbackend.domain.album.dto.response.InviteLinkResponse;
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
    public ResponseEntity<CreateAlbumResponse> createAlbum(@RequestBody CreateAlbumRequest request) {
        AlbumEntity album = albumService.createAlbum(
                request.getUserId(),
                request.getRatio(),
                request.getTargetPages(),
                request.getCoverLayersJson(),
                request.getCoverTheme(),
                request.getCoverOriginalUrl(),
                request.getCoverPreviewUrl(),
                request.getCoverImageUrl(),
                request.getCoverThumbnailUrl()
        );

        CreateAlbumResponse response = CreateAlbumResponse.builder()
                .albumId(album.getId())
                .ratio(album.getRatio())
                .targetPages(album.getTargetPages())
                .coverLayersJson(album.getCoverLayersJson())
                .coverTheme(album.getCoverTheme())
                .coverOriginalUrl(album.getCoverOriginalUrl())
                .coverPreviewUrl(album.getCoverPreviewUrl())
                .coverImageUrl(album.getCoverImageUrl())
                .coverThumbnailUrl(album.getCoverThumbnailUrl())
                .createdAt(album.getCreatedAt())
                .updatedAt(album.getUpdatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내가 저장한 앨범 목록 조회 (userId 필터)
     */
    @Operation(summary = "내 앨범 목록 조회")
    @GetMapping
    public ResponseEntity<List<AlbumListResponse>> getMyAlbums(
            @RequestParam String userId
    ) {
        List<AlbumEntity> albums = albumService.getAlbumsByUserId(userId);
        List<AlbumListResponse> response = albums.stream()
                .map(a -> AlbumListResponse.builder()
                        .albumId(a.getId())
                        .ratio(a.getRatio())
                        .coverThumbnailUrl(a.getCoverThumbnailUrl())
                        .coverImageUrl(a.getCoverImageUrl())
                        .totalPages(a.getTotalPages())
                        .targetPages(a.getTargetPages())
                        .createdAt(a.getCreatedAt())
                        .updatedAt(a.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범의 페이지 리스트 조회 (소유자만)
     */
    @Operation(summary = "앨범 페이지 목록 조회")
    @GetMapping("/{albumId}/pages")
    public ResponseEntity<List<AlbumPageResponse>> getAlbumPages(
            @PathVariable Long albumId,
            @RequestParam String userId
    ) {
        List<AlbumPageEntity> pages = albumService.getAlbumPages(albumId, userId);

        List<AlbumPageResponse> response = pages.stream()
                .map(page -> AlbumPageResponse.builder()
                        .pageId(page.getId())
                        .albumId(page.getAlbum().getId())
                        .pageNumber(page.getPageNumber())
                        .layersJson(page.getLayersJson())
                        .originalUrl(page.getOriginalUrl())
                        .previewUrl(page.getPreviewUrl())
                        .imageUrl(page.getImageUrl())
                        .thumbnailUrl(page.getThumbnailUrl())
                        .build()
                )
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 페이지 저장/업데이트 (소유자만)
     */
    @Operation(summary = "앨범 페이지 저장(업서트)")
    @PostMapping("/{albumId}/pages")
    public ResponseEntity<AlbumPageResponse> savePage(
            @PathVariable Long albumId,
            @RequestParam String userId,
            @RequestBody SaveAlbumPageRequest request
    ) {
        AlbumPageEntity page = albumService.savePage(
                albumId,
                request.getPageNumber(),
                request.getLayersJson(),
                (request.getPreviewUrl() != null && !request.getPreviewUrl().isBlank())
                        ? request.getPreviewUrl()
                        : request.getImageUrl(),
                request.getThumbnailUrl(),
                userId
        );

        AlbumPageResponse response = AlbumPageResponse.builder()
                .pageId(page.getId())
                .albumId(page.getAlbum().getId())
                .pageNumber(page.getPageNumber())
                .layersJson(page.getLayersJson())
                .originalUrl(page.getOriginalUrl())
                .previewUrl(page.getPreviewUrl())
                .imageUrl(page.getImageUrl())
                .thumbnailUrl(page.getThumbnailUrl())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 수정 (소유자만)
     * - Body: CreateAlbumRequest와 동일 (ratio, coverLayersJson, coverImageUrl, coverThumbnailUrl, userId)
     */
    @Operation(summary = "앨범 수정")
    @PutMapping("/{albumId}")
    public ResponseEntity<CreateAlbumResponse> updateAlbum(
            @PathVariable Long albumId,
            @RequestParam String userId,
            @RequestBody CreateAlbumRequest request
    ) {
        AlbumEntity album = albumService.updateAlbum(
                albumId,
                userId,
                request.getRatio(),
                request.getTargetPages(),
                request.getCoverLayersJson(),
                request.getCoverTheme(),
                request.getCoverOriginalUrl(),
                request.getCoverPreviewUrl(),
                request.getCoverImageUrl(),
                request.getCoverThumbnailUrl()
        );
        CreateAlbumResponse response = CreateAlbumResponse.builder()
                .albumId(album.getId())
                .ratio(album.getRatio())
                .targetPages(album.getTargetPages())
                .coverLayersJson(album.getCoverLayersJson())
                .coverTheme(album.getCoverTheme())
                .coverOriginalUrl(album.getCoverOriginalUrl())
                .coverPreviewUrl(album.getCoverPreviewUrl())
                .coverImageUrl(album.getCoverImageUrl())
                .coverThumbnailUrl(album.getCoverThumbnailUrl())
                .createdAt(album.getCreatedAt())
                .updatedAt(album.getUpdatedAt())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 삭제 (소유자만)
     */
    @Operation(summary = "앨범 삭제")
    @DeleteMapping("/{albumId}")
    public ResponseEntity<Void> deleteAlbum(
            @PathVariable Long albumId,
            @RequestParam String userId
    ) {
        albumService.deleteAlbum(albumId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 앨범 멤버 초대 링크 생성 (소유자만)
     */
    @Operation(summary = "앨범 멤버 초대 링크 생성")
    @PostMapping("/{albumId}/members/invite")
    public ResponseEntity<InviteLinkResponse> inviteMember(
            @PathVariable Long albumId,
            @RequestParam String userId,
            @RequestBody InviteAlbumRequest request
    ) {
        AlbumMemberRole role = AlbumMemberRole.EDITOR;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            role = AlbumMemberRole.valueOf(request.getRole());
        }
        AlbumMemberEntity invite = albumService.inviteMember(albumId, userId, role);
        InviteLinkResponse response = InviteLinkResponse.builder()
                .albumId(albumId)
                .token(invite.getInviteToken())
                .link("/invite?token=" + invite.getInviteToken())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 멤버 목록 조회
     */
    @Operation(summary = "앨범 멤버 목록 조회")
    @GetMapping("/{albumId}/members")
    public ResponseEntity<List<AlbumMemberResponse>> getMembers(
            @PathVariable Long albumId,
            @RequestParam String userId
    ) {
        List<AlbumMemberEntity> members = albumService.getMembers(albumId, userId);
        List<AlbumMemberResponse> response = members.stream()
                .map(m -> AlbumMemberResponse.builder()
                        .id(m.getId())
                        .albumId(m.getAlbum().getId())
                        .userId(m.getUserId())
                        .role(m.getRole().name())
                        .status(m.getStatus().name())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 상세 조회 (앨범 + 페이지 목록, 소유자만)
     */
    @Operation(summary = "앨범 상세 조회")
    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumDetailResponse> getAlbumDetail(
            @PathVariable Long albumId,
            @RequestParam String userId
    ) {
        // 1) 앨범, 페이지 조회 (소유자 검증 포함)
        AlbumEntity album = albumService.getAlbumForMember(albumId, userId);
        List<AlbumPageEntity> pages = albumService.getAlbumPages(albumId, userId);

        // 2) 페이지 요약 DTO로 매핑
        List<AlbumDetailResponse.AlbumPageSummaryResponse> pageResponses = pages.stream()
                .map(page -> AlbumDetailResponse.AlbumPageSummaryResponse.builder()
                        .pageId(page.getId())
                        .pageNumber(page.getPageNumber())
                        .imageUrl(page.getPreviewUrl() != null ? page.getPreviewUrl() : page.getImageUrl())
                        .thumbnailUrl(page.getThumbnailUrl())
                        .build()
                )
                .collect(Collectors.toList());

        // 3) 앨범 상세 DTO 구성
        AlbumDetailResponse response = AlbumDetailResponse.builder()
                .albumId(album.getId())
                .ratio(album.getRatio())
                .coverLayersJson(album.getCoverLayersJson())
                .coverTheme(album.getCoverTheme())
                .coverOriginalUrl(album.getCoverOriginalUrl())
                .coverPreviewUrl(album.getCoverPreviewUrl())
                .coverImageUrl(album.getCoverImageUrl())
                .albumThumbnailUrl(album.getCoverThumbnailUrl())
                .totalPages(album.getTotalPages())
                .targetPages(album.getTargetPages())
                .createdAt(album.getCreatedAt())
                .updatedAt(album.getUpdatedAt())
                .pages(pageResponses)
                .build();

        return ResponseEntity.ok(response);
    }
}