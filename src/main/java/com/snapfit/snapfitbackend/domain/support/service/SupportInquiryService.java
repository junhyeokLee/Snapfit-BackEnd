package com.snapfit.snapfitbackend.domain.support.service;

import com.snapfit.snapfitbackend.domain.support.entity.SupportInquiryEntity;
import com.snapfit.snapfitbackend.domain.support.entity.SupportInquiryStatus;
import com.snapfit.snapfitbackend.domain.support.repository.SupportInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupportInquiryService {

    private final SupportInquiryRepository supportInquiryRepository;

    @Transactional
    public Map<String, Object> create(String userId, String category, String subject, String message) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is required");
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("subject is required");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message is required");

        SupportInquiryEntity saved = supportInquiryRepository.save(SupportInquiryEntity.builder()
                .userId(userId.trim())
                .category((category == null || category.isBlank()) ? "GENERAL" : category.trim())
                .subject(subject.trim())
                .message(message.trim())
                .status(SupportInquiryStatus.OPEN)
                .build());
        return toRow(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listAdmin(String status, String keyword, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        SupportInquiryStatus parsedStatus = parseStatus(status);
        String query = keyword == null ? "" : keyword.trim();

        Page<SupportInquiryEntity> result = supportInquiryRepository.searchAdmin(parsedStatus, query, pageable);
        return Map.of(
                "items", result.getContent().stream().map(this::toRow).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "hasNext", result.hasNext()
        );
    }

    @Transactional
    public Map<String, Object> resolve(Long id, String resolvedBy) {
        SupportInquiryEntity inquiry = supportInquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("inquiry not found: " + id));
        inquiry.setStatus(SupportInquiryStatus.RESOLVED);
        inquiry.setResolvedAt(LocalDateTime.now());
        inquiry.setResolvedBy((resolvedBy == null || resolvedBy.isBlank()) ? "admin" : resolvedBy.trim());
        return toRow(supportInquiryRepository.save(inquiry));
    }

    private SupportInquiryStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return SupportInquiryStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> toRow(SupportInquiryEntity e) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", e.getId());
        row.put("userId", e.getUserId());
        row.put("category", e.getCategory());
        row.put("subject", e.getSubject());
        row.put("message", e.getMessage());
        row.put("status", e.getStatus().name());
        row.put("resolvedAt", e.getResolvedAt());
        row.put("resolvedBy", e.getResolvedBy() == null ? "" : e.getResolvedBy());
        row.put("createdAt", e.getCreatedAt());
        row.put("updatedAt", e.getUpdatedAt());
        return row;
    }
}
