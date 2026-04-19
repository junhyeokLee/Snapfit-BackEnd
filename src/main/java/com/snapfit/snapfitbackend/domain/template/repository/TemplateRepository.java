package com.snapfit.snapfitbackend.domain.template.repository;

import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {
    @Query("select t from TemplateEntity t where t.active = true or t.active is null")
    Page<TemplateEntity> findAllActive(Pageable pageable);

    @Query("select count(t) from TemplateEntity t where t.active = true or t.active is null")
    long countActive();

    long countByActiveFalse();
}
