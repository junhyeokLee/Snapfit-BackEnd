package com.snapfit.snapfitbackend.domain.support.repository;

import com.snapfit.snapfitbackend.domain.support.entity.SupportInquiryEntity;
import com.snapfit.snapfitbackend.domain.support.entity.SupportInquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportInquiryRepository extends JpaRepository<SupportInquiryEntity, Long> {

    @Query("""
            select i
            from SupportInquiryEntity i
            where (:status is null or i.status = :status)
              and (
                  :keyword = '' or
                  lower(i.userId) like lower(concat('%', :keyword, '%')) or
                  lower(i.subject) like lower(concat('%', :keyword, '%')) or
                  lower(i.message) like lower(concat('%', :keyword, '%'))
              )
            order by i.updatedAt desc
            """)
    Page<SupportInquiryEntity> searchAdmin(
            @Param("status") SupportInquiryStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

