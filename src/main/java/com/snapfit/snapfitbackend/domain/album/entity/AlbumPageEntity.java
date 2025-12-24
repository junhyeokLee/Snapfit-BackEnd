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
     * 이 페이지를 한 장의 이미지로 렌더링한 URL (인쇄용/미리보기용 원본)
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

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