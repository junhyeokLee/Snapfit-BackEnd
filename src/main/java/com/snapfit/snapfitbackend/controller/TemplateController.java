package com.snapfit.snapfitbackend.controller;

import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.template.dto.response.TemplateResponse;
import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import com.snapfit.snapfitbackend.domain.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    @Value("${snapfit.push.admin-key:}")
    private String adminKey;

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(templateService.getAllTemplates(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplateDetail(
            @PathVariable Long id,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(templateService.getTemplateDetail(id, userId));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeTemplate(
            @PathVariable Long id,
            @RequestParam String userId) {
        templateService.likeTemplate(id, userId);
        return ResponseEntity.ok().build();
    }

    // Create album from template
    // POST /api/albums/from-template/{templateId}?userId=... is messy if we put it
    // in AlbumController
    // Let's put it here or make a new endpoint in AlbumController.
    // The implementation plan said AlbumController, but I'll put it here for
    // cohesion with TemplateService.
    // Actually, "creating an album" belongs to Album domain conceptually, but
    // "using a template" starts here.
    // I will put it here: POST /api/templates/{id}/use?userId=... -> Returns
    // created Album

    @PostMapping("/{id}/use")
    public ResponseEntity<AlbumEntity> createAlbumFromTemplate(
            @PathVariable Long id,
            @RequestParam String userId,
            @RequestBody(required = false) java.util.Map<String, String> replacements) {
        AlbumEntity album = templateService.createAlbumFromTemplate(id, userId, replacements);
        return ResponseEntity.ok(album);
    }

    @PostMapping("/admin/upsert")
    public ResponseEntity<?> upsertTemplate(
            @RequestHeader(value = "X-Admin-Key", required = false) String requestAdminKey,
            @RequestBody TemplateUpsertRequest request) {
        if (adminKey == null || adminKey.isBlank() || !adminKey.equals(requestAdminKey)) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        }
        TemplateEntity saved = templateService.upsertTemplateFromAdmin(request.toEntity());
        return ResponseEntity.ok(Map.of("id", saved.getId()));
    }

    @lombok.Data
    public static class TemplateUpsertRequest {
        private Long id;
        private String title;
        private String subTitle;
        private String description;
        private String coverImageUrl;
        private String previewImagesJson;
        private int pageCount;
        private int likeCount;
        private int userCount;
        private boolean isBest;
        private boolean isPremium;
        private String category;
        private String tagsJson;
        private Integer weeklyScore;
        private java.time.LocalDateTime newUntil;
        private Boolean active;
        private String templateJson;

        public TemplateEntity toEntity() {
            return TemplateEntity.builder()
                    .id(id)
                    .title(title)
                    .subTitle(subTitle)
                    .description(description)
                    .coverImageUrl(coverImageUrl)
                    .previewImagesJson(previewImagesJson)
                    .pageCount(pageCount)
                    .likeCount(likeCount)
                    .userCount(userCount)
                    .isBest(isBest)
                    .isPremium(isPremium)
                    .category(category)
                    .tagsJson(tagsJson)
                    .weeklyScore(weeklyScore)
                    .newUntil(newUntil)
                    .active(active)
                    .templateJson(templateJson)
                    .build();
        }
    }
}
