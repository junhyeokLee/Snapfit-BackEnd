package com.snapfit.snapfitbackend.domain.template.repository;

import com.snapfit.snapfitbackend.domain.template.entity.TemplateLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateLikeRepository extends JpaRepository<TemplateLikeEntity, Long> {
    Optional<TemplateLikeEntity> findByTemplateIdAndUserId(Long templateId, String userId);

    boolean existsByTemplateIdAndUserId(Long templateId, String userId);

    long deleteByUserId(String userId);

    long deleteByTemplateId(Long templateId);
}
