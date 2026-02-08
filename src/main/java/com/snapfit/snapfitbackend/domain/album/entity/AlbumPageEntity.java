package com.snapfit.snapfitbackend.domain.album.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "album_page")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AlbumPageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 어느 앨범의 페이지인지
     * - 다(N) : 1(Album)
     * - JSON 순환참조 방지를 위해 @JsonBackReference 사용
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    @JsonBackReference
    private AlbumEntity album;

    /**
     * 1,2,3,... (실제 페이지 번호)
     */
    @Column(nullable = false)
    private int pageNumber;

    /**
     * Flutter LayerModel[] 전체를 JSON 으로 직렬화해서 저장
     */
    @Lob
    @Column(name = "layers_json", columnDefinition = "LONGTEXT", nullable = false)
    private String layersJson;

    /**
     * (구) 이 페이지를 한 장의 이미지로 렌더링한 URL
     * - 하위 호환을 위해 previewUrl을 미러링해서 저장할 수 있습니다.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 페이지 원본(프린트용) URL
     */
    @Column(name = "original_url", length = 1000)
    private String originalUrl;

    /**
     * 페이지 미리보기(앱용) URL
     */
    @Column(name = "preview_url", length = 1000)
    private String previewUrl;

    /**
     * 리스트 / 하단 썸네일 바에서 보여줄 작은 이미지 URL
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}