package com.snapfit.snapfitbackend.domain.template.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "template_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "template_id", "user_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TemplateLikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @CreatedDate
    private LocalDateTime createdAt;
}
