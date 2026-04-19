package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.support.service.SupportInquiryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportInquiryController {

    private final SupportInquiryService supportInquiryService;

    @PostMapping("/inquiries")
    public ResponseEntity<Map<String, Object>> createInquiry(@RequestBody CreateInquiryRequest request) {
        return ResponseEntity.ok(supportInquiryService.create(
                request.userId,
                request.category,
                request.subject,
                request.message
        ));
    }

    @Data
    public static class CreateInquiryRequest {
        private String userId;
        private String category;
        private String subject;
        private String message;
    }
}

