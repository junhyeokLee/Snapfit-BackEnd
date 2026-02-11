package com.snapfit.snapfitbackend.domain.album.service;

import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberRole;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumMemberStatus;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumPageEntity;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumMemberRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumPageRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumRepository;
import com.snapfit.snapfitbackend.domain.image.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumPageRepository albumPageRepository;
    private final AlbumMemberRepository albumMemberRepository;
    private final ImageStorageService imageStorageService;

    /**
     * 앨범 생성
     * - 제목은 사용하지 않고, 비율 + 커버 레이어 JSON + 커버/썸네일 URL만 저장
     * - createdAt/updatedAt, totalPages 는 엔티티 @PrePersist 에서 처리
     */
    @Transactional
    public AlbumEntity createAlbum(
            String userId,
            String ratio,
            Integer targetPages,
            String coverLayersJson,
            String coverTheme,
            String coverOriginalUrl,
            String coverPreviewUrl,
            String coverImageUrl,
            String coverThumbnailUrl
    ) {
        // 엔티티 빌더로 앨범 생성
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        // 하위 호환: 새 필드 미전달 시 기존 coverImageUrl 값을 preview로 간주
        String resolvedCoverPreviewUrl = (coverPreviewUrl != null && !coverPreviewUrl.isBlank())
                ? coverPreviewUrl
                : coverImageUrl;

        AlbumEntity album = AlbumEntity.builder()
                .userId(userId)
                .ratio(ratio)
                .targetPages(targetPages == null ? 0 : targetPages)
                .coverLayersJson(coverLayersJson)      // 커버 레이어 JSON
                .coverTheme(coverTheme)                // 커버 테마(배경)
                .coverOriginalUrl(coverOriginalUrl)    // 커버 원본(프린트용)
                .coverPreviewUrl(resolvedCoverPreviewUrl) // 커버 미리보기(앱용)
                .coverImageUrl(resolvedCoverPreviewUrl)   // (구) 커버 이미지 URL = preview 미러링
                .coverThumbnailUrl(coverThumbnailUrl)  // 목록용 썸네일 (없으면 null 허용)
                .build();

        AlbumEntity saved = albumRepository.save(album);

        // 소유자를 멤버로 등록
        try {
            AlbumMemberEntity owner = AlbumMemberEntity.builder()
                    .album(saved)
                    .userId(userId)
                    .role(AlbumMemberRole.OWNER)
                    .status(AlbumMemberStatus.ACCEPTED)
                    .invitedBy(userId)
                    .build();
            albumMemberRepository.save(owner);
        } catch (Exception e) {
            // 운영 환경에서 멤버 테이블이 아직 없을 수 있어 생성은 계속 진행
            // (멤버 기능은 이후 마이그레이션 완료 시 정상 동작)
        }

        return saved;
    }

    /**
     * 특정 사용자가 저장한 앨범 목록 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<AlbumEntity> getAlbumsByUserId(String userId) {
        try {
            return albumRepository.findAllAccessibleByUser(userId);
        } catch (Exception e) {
            return albumRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
    }

    /**
     * 앨범의 모든 페이지 조회 (소유자 검증)
     */
    @Transactional(readOnly = true)
    public List<AlbumPageEntity> getAlbumPages(Long albumId, String userId) {
        getAlbumForMember(albumId, userId); // 멤버/소유자 검증
        return albumPageRepository.findByAlbumId(albumId);
    }

    /**
     * 페이지 저장 (업서트: 있으면 업데이트, 없으면 새로 생성)
     * - 앨범 소유자만 저장 가능
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
            String thumbnailUrl,
            String userId
    ) {
        // 1) 앨범 엔티티 조회 + 편집 권한 검증
        AlbumEntity album = assertCanEdit(albumId, userId);

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
        // 하위 호환: 기존 renderImageUrl은 previewUrl로 간주
        page.setPreviewUrl(renderImageUrl);
        page.setImageUrl(renderImageUrl); // (구) 필드 미러링
        page.setThumbnailUrl(thumbnailUrl);

        // 4) 저장
        AlbumPageEntity saved = albumPageRepository.save(page);

        // 5) 페이지 수 캐시 갱신
        long pageCount = albumPageRepository.countByAlbumId(albumId);
        album.setTotalPages((int) pageCount);

        return saved;
    }

    /**
     * 앨범 단건 조회 (소유자만 조회 가능)
     */
    @Transactional(readOnly = true)
    public AlbumEntity getAlbum(Long albumId, String userId) {
        AlbumEntity album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범을 찾을 수 없습니다. id=" + albumId));
        if (!album.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 앨범에 대한 권한이 없습니다.");
        }
        return album;
    }

    @Transactional(readOnly = true)
    public AlbumEntity getAlbumForMember(Long albumId, String userId) {
        AlbumEntity album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범을 찾을 수 없습니다. id=" + albumId));
        if (album.getUserId().equals(userId)) {
            return album;
        }
        boolean isMember = albumMemberRepository
                .existsByAlbumIdAndUserIdAndStatus(albumId, userId, AlbumMemberStatus.ACCEPTED);
        if (!isMember) {
            throw new IllegalArgumentException("해당 앨범에 대한 권한이 없습니다.");
        }
        return album;
    }

    private AlbumEntity assertCanEdit(Long albumId, String userId) {
        AlbumEntity album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범을 찾을 수 없습니다. id=" + albumId));
        if (album.getUserId().equals(userId)) {
            return album;
        }
        AlbumMemberEntity member = albumMemberRepository
                .findByAlbumIdAndUserId(albumId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 앨범에 대한 권한이 없습니다."));
        if (member.getStatus() != AlbumMemberStatus.ACCEPTED) {
            throw new IllegalArgumentException("해당 앨범에 대한 권한이 없습니다.");
        }
        if (member.getRole() != AlbumMemberRole.EDITOR && member.getRole() != AlbumMemberRole.OWNER) {
            throw new IllegalArgumentException("해당 앨범에 대한 권한이 없습니다.");
        }
        return album;
    }

    @Transactional
    public AlbumMemberEntity inviteMember(Long albumId, String ownerId, AlbumMemberRole role) {
        AlbumEntity album = getAlbum(albumId, ownerId);
        String token = UUID.randomUUID().toString().replace("-", "");
        AlbumMemberEntity member = AlbumMemberEntity.builder()
                .album(album)
                .role(role == null ? AlbumMemberRole.EDITOR : role)
                .status(AlbumMemberStatus.PENDING)
                .invitedBy(ownerId)
                .inviteToken(token)
                .build();
        return albumMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public AlbumMemberEntity getInvite(String token) {
        return albumMemberRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("초대 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public AlbumMemberEntity acceptInvite(String token, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        AlbumMemberEntity invite = albumMemberRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("초대 정보를 찾을 수 없습니다."));
        if (invite.getStatus() == AlbumMemberStatus.ACCEPTED) {
            throw new IllegalArgumentException("이미 수락된 초대입니다.");
        }
        boolean alreadyMember = albumMemberRepository
                .existsByAlbumIdAndUserIdAndStatus(invite.getAlbum().getId(), userId, AlbumMemberStatus.ACCEPTED);
        if (alreadyMember) {
            invite.setStatus(AlbumMemberStatus.ACCEPTED);
            invite.setInviteToken(null);
            return albumMemberRepository.save(invite);
        }
        invite.setUserId(userId);
        invite.setStatus(AlbumMemberStatus.ACCEPTED);
        invite.setInviteToken(null);
        return albumMemberRepository.save(invite);
    }

    @Transactional(readOnly = true)
    public List<AlbumMemberEntity> getMembers(Long albumId, String requesterId) {
        getAlbumForMember(albumId, requesterId);
        return albumMemberRepository.findByAlbumId(albumId);
    }

    /**
     * 앨범 수정 (소유자만)
     * - CreateAlbumRequest와 동일한 필드(ratio, coverLayersJson, coverImageUrl, coverThumbnailUrl)로 갱신
     */
    @Transactional
    public AlbumEntity updateAlbum(
            Long albumId,
            String userId,
            String ratio,
            Integer targetPages,
            String coverLayersJson,
            String coverTheme,
            String coverOriginalUrl,
            String coverPreviewUrl,
            String coverImageUrl,
            String coverThumbnailUrl
    ) {
        AlbumEntity album = getAlbum(albumId, userId);
        album.setRatio(ratio);
        if (targetPages != null && targetPages > 0) {
            album.setTargetPages(targetPages);
        }
        album.setCoverLayersJson(coverLayersJson);
        album.setCoverTheme(coverTheme);

        // 하위 호환: 새 필드 미전달 시 기존 coverImageUrl 값을 preview로 간주
        String resolvedCoverPreviewUrl = (coverPreviewUrl != null && !coverPreviewUrl.isBlank())
                ? coverPreviewUrl
                : coverImageUrl;

        album.setCoverOriginalUrl(coverOriginalUrl);
        album.setCoverPreviewUrl(resolvedCoverPreviewUrl);
        album.setCoverImageUrl(resolvedCoverPreviewUrl); // (구) 필드 미러링
        album.setCoverThumbnailUrl(coverThumbnailUrl);
        return albumRepository.save(album);
    }

    /**
     * 앨범 삭제 (소유자만 삭제 가능)
     *
     * <p>왜 삭제 시 스토리지까지 정리하나?</p>
     * - 사용자가 앨범을 삭제했는데 Firebase Storage/S3에 고해상도 원본이 계속 남아 있으면
     *   스토리지 비용이 쌓이고, 개인정보/프라이버시 측면에서도 "완전 삭제" 기대와 어긋납니다.
     * - 그렇다고 단순히 URL 모두를 무조건 delete 하면, 여러 앨범에서 같은 이미지를 재사용하는
     *   경우에 다른 앨범 화면이 깨질 수 있습니다.
     * - 그래서 이 메서드에서는:
     *   1) 이 앨범이 참조하는 모든 이미지 URL을 수집하고
     *   2) 앨범/페이지 레코드를 삭제한 뒤
     *   3) 동일 URL을 참조하는 다른 앨범/페이지가 더 이상 없는 경우에만 실제 스토리지에서 삭제합니다.
     */
    @Transactional
    public void deleteAlbum(Long albumId, String userId) {
        AlbumEntity album = getAlbum(albumId, userId); // 소유자 검증 포함

        // 1) 삭제 전에 이 앨범이 참조하는 모든 이미지 URL 수집
        List<AlbumPageEntity> pages = albumPageRepository.findByAlbumId(albumId);

        java.util.Set<String> urls = new java.util.HashSet<>();
        // 커버 쪽
        urls.add(album.getCoverOriginalUrl());
        urls.add(album.getCoverPreviewUrl());
        urls.add(album.getCoverImageUrl());
        urls.add(album.getCoverThumbnailUrl());

        // 페이지 쪽
        for (AlbumPageEntity page : pages) {
            urls.add(page.getOriginalUrl());
            urls.add(page.getPreviewUrl());
            urls.add(page.getImageUrl());
            urls.add(page.getThumbnailUrl());
        }

        // 2) 앨범/페이지 레코드 삭제 (cascade)
        albumRepository.delete(album);

        // 3) 실제 스토리지에서 삭제: 더 이상 어떤 앨범/페이지에서도 사용하지 않는 URL만 삭제
        for (String url : urls) {
            if (url == null || url.isBlank()) continue;

            boolean usedByAlbum =
                    albumRepository.existsByCoverOriginalUrl(url)
                            || albumRepository.existsByCoverPreviewUrl(url)
                            || albumRepository.existsByCoverImageUrl(url)
                            || albumRepository.existsByCoverThumbnailUrl(url);

            boolean usedByPage =
                    albumPageRepository.existsByOriginalUrl(url)
                            || albumPageRepository.existsByPreviewUrl(url)
                            || albumPageRepository.existsByImageUrl(url)
                            || albumPageRepository.existsByThumbnailUrl(url);

            if (!usedByAlbum && !usedByPage) {
                imageStorageService.delete(url);
            }
        }
    }
}