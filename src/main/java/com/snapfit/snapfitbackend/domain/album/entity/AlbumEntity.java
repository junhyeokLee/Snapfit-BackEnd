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
     * 커버/페이지 비율
     * 예) "3:4", "1:1", "A4"
     */
    @Column(nullable = false, length = 20)
    private String ratio;

    /**
     * 커버 대표 이미지 URL (렌더링된 최종 이미지)
     */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /**
     * 앨범 전체 썸네일(목록용)
     */
    @Column(name = "cover_thumbnail_url", length = 500)
    private String coverThumbnailUrl;

    /**
     * 커버 레이어 JSON
     * LayerModel[] 형태 그대로 Flutter → 백엔드 저장
     */
    @Lob
    @Column(name = "cover_layers_json", columnDefinition = "LONGTEXT")
    private String coverLayersJson;

    /**
     * 페이지 개수 캐시
     */
    @Column(name = "total_pages")
    private Integer totalPages;

    /**
     * 앨범에 속한 페이지들
     */
    @OneToMany(
            mappedBy = "album",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
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