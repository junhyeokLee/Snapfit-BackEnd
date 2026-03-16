package com.snapfit.snapfitbackend.domain.template.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String subTitle;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String coverImageUrl;

    @Column(columnDefinition = "LONGTEXT")
    private String previewImagesJson; // JSON array of strings

    private int pageCount;

    private int likeCount;

    private int userCount;

    private boolean isBest;

    private boolean isPremium;

    // This JSON stores the full structure to create an album (pages, layers, etc.)
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String templateJson;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = java.time.LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
}