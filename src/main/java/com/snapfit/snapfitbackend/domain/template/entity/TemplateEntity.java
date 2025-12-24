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

    private String name;

    private String previewUrl;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String templateJson;
}