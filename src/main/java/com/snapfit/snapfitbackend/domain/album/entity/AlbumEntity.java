package com.snapfit.snapfitbackend.domain.album.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "album")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AlbumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 앨범 소유자 식별자 (예: Firebase UID 등)
     * - 이 사용자가 저장한 앨범만 조회할 때 사용
     */
    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    /**
     * 커버/페이지 비율
     * 예) "3:4", "1:1", "A4"
     */
    @Column(name = "ratio", nullable = false, length = 100)
    private String ratio;

    @Column(name = "cover_image_url", length = 1000) // 500 -> 1000으로 확장
    private String coverImageUrl;

    @Column(name = "cover_thumbnail_url", length = 1000) // 500 -> 1000으로 확장
    private String coverThumbnailUrl;

    /**
     * 커버 원본(프린트용) URL
     */
    @Column(name = "cover_original_url", length = 1000)
    private String coverOriginalUrl;

    /**
     * 커버 미리보기(앱용) URL
     * - 하위 호환을 위해 coverImageUrl에도 동일 값을 미러링할 수 있습니다.
     */
    @Column(name = "cover_preview_url", length = 1000)
    private String coverPreviewUrl;

    // @Lob은 환경에 따라 문제를 일으킬 수 있으므로 columnDefinition만 명시하는 것이 안전할 때가 많습니다.
    @Column(name = "cover_layers_json", columnDefinition = "LONGTEXT")
    private String coverLayersJson;

    /**
     * 커버 테마(배경) 식별자
     * - coverLayersJson(레이어)와 분리 저장/복원
     * - 예) "classic"
     */
    @Column(name = "cover_theme", length = 100)
    private String coverTheme;

    /**
     * 페이지 개수 캐시
     */
    @Column(name = "total_pages")
    private Integer totalPages;

    /**
     * 목표 페이지 수 (완성 기준)
     */
    @Column(name = "target_pages")
    private Integer targetPages;

    /**
     * 앨범에 속한 페이지들
     */
    @Builder.Default
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<AlbumPageEntity> pages = new ArrayList<>();

    /**
     * 생성/수정 시간
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.totalPages == null) {
            this.totalPages = 0;
        }
        if (this.targetPages == null) {
            this.targetPages = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==========================
    // 연관관계 편의 메서드
    // ==========================

    public void addPage(AlbumPageEntity page) {
        if (page == null) return;
        this.pages.add(page);
        page.setAlbum(this);
        this.totalPages = this.pages.size();
    }

    public void removePage(AlbumPageEntity page) {
        if (page == null) return;
        this.pages.remove(page);
        page.setAlbum(null);
        this.totalPages = this.pages.size();
    }
}