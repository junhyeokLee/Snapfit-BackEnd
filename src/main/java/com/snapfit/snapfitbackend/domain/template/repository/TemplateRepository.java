package com.snapfit.snapfitbackend.domain.template.repository;

import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {
}