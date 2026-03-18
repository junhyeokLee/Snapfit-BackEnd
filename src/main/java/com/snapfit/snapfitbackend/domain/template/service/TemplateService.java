package com.snapfit.snapfitbackend.domain.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.service.AlbumService;
import com.snapfit.snapfitbackend.domain.template.dto.response.TemplateResponse;
import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import com.snapfit.snapfitbackend.domain.template.entity.TemplateLikeEntity;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateLikeRepository;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateLikeRepository templateLikeRepository;
    private final AlbumService albumService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates(String userId) {
        return templateRepository.findAll().stream()
                .filter(this::isActive)
                .sorted(this::compareTemplateRanking)
                .map(entity -> {
                    boolean isLiked = false;
                    if (userId != null && !userId.isEmpty()) {
                        isLiked = templateLikeRepository.existsByTemplateIdAndUserId(entity.getId(), userId);
                    }
                    return convertToResponse(entity, isLiked);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateDetail(Long id, String userId) {
        TemplateEntity template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        if (!isActive(template)) {
            throw new IllegalArgumentException("Template is inactive: " + id);
        }

        boolean isLiked = false;
        if (userId != null && !userId.isEmpty()) {
            isLiked = templateLikeRepository.existsByTemplateIdAndUserId(id, userId);
        }
        return convertToResponse(template, isLiked);
    }

    @Transactional
    public void likeTemplate(Long id, String userId) {
        TemplateEntity template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        Optional<TemplateLikeEntity> existingLike = templateLikeRepository.findByTemplateIdAndUserId(id, userId);

        if (existingLike.isPresent()) {
            // Unlike
            templateLikeRepository.delete(existingLike.get());
            template.setLikeCount(Math.max(0, template.getLikeCount() - 1));
        } else {
            // Like
            templateLikeRepository.save(TemplateLikeEntity.builder()
                    .templateId(id)
                    .userId(userId)
                    .build());
            template.setLikeCount(template.getLikeCount() + 1);
        }
        templateRepository.save(template);
    }

    @Transactional
    public AlbumEntity createAlbumFromTemplate(Long templateId, String userId,
            Map<String, String> replacements) {
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        if (!isActive(template)) {
            throw new IllegalArgumentException("Template is inactive: " + templateId);
        }

        // 1. Parse templateJson to extract album structure
        JsonNode root;
        try {
            root = objectMapper.readTree(template.getTemplateJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse template JSON", e);
        }

        // Apply replacements if provided
        if (replacements != null && !replacements.isEmpty()) {
            applyReplacements(root, replacements);
        }

        String ratio = root.path("ratio").asText("1:1"); // Default to 1:1 if missing
        JsonNode coverNode = root.path("cover");
        String coverLayersJson = coverNode.path("layers").toString();
        String coverTheme = coverNode.path("theme").asText("default");

        // 2. Create Album Shell
        AlbumEntity album = albumService.createAlbum(
                userId,
                ratio,
                template.getTitle(),
                template.getPageCount(),
                coverLayersJson,
                coverTheme,
                template.getCoverImageUrl(),
                template.getCoverImageUrl(),
                template.getCoverImageUrl(),
                template.getCoverImageUrl());

        // 3. Create Pages
        JsonNode pagesNode = root.path("pages");
        if (pagesNode.isArray()) {
            for (JsonNode pageNode : pagesNode) {
                int pageNumber = pageNode.path("pageNumber").asInt();
                String layersJson = pageNode.path("layers").toString();

                albumService.savePage(
                        album.getId(),
                        pageNumber,
                        layersJson,
                        null,
                        null,
                        userId);
            }
        }

        template.setUserCount(template.getUserCount() + 1);
        templateRepository.save(template);

        return album;
    }

    @Transactional
    public TemplateEntity upsertTemplateFromAdmin(TemplateEntity input) {
        validateTemplateInput(input);
        TemplateEntity target;
        if (input.getId() != null) {
            target = templateRepository.findById(input.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + input.getId()));
        } else {
            target = new TemplateEntity();
        }

        target.setTitle(input.getTitle().trim());
        target.setSubTitle(input.getSubTitle());
        target.setDescription(input.getDescription());
        target.setCoverImageUrl(input.getCoverImageUrl());
        target.setPreviewImagesJson(input.getPreviewImagesJson());
        target.setPageCount(input.getPageCount());
        target.setLikeCount(Math.max(0, input.getLikeCount()));
        target.setUserCount(Math.max(0, input.getUserCount()));
        target.setBest(input.isBest());
        target.setPremium(input.isPremium());
        target.setCategory(normalizeCategory(input.getCategory()));
        target.setTagsJson(input.getTagsJson());
        target.setWeeklyScore(input.getWeeklyScore() == null ? 0 : Math.max(0, input.getWeeklyScore()));
        target.setNewUntil(input.getNewUntil());
        target.setActive(input.getActive() == null ? true : input.getActive());
        target.setTemplateJson(input.getTemplateJson());
        return templateRepository.save(target);
    }

    private void validateTemplateInput(TemplateEntity input) {
        if (input.getTitle() == null || input.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("title is required");
        }
        if (input.getCoverImageUrl() == null || input.getCoverImageUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("coverImageUrl is required");
        }
        if (input.getPageCount() <= 0) {
            throw new IllegalArgumentException("pageCount must be greater than 0");
        }
        if (input.getTemplateJson() == null || input.getTemplateJson().trim().isEmpty()) {
            throw new IllegalArgumentException("templateJson is required");
        }
        ensureValidTemplateJson(input.getTemplateJson());
        ensureValidJsonArray(input.getPreviewImagesJson(), "previewImagesJson");
        ensureValidJsonArray(input.getTagsJson(), "tagsJson");
    }

    private void ensureValidTemplateJson(String templateJson) {
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            if (root.path("cover").isMissingNode() || !root.path("cover").path("layers").isArray()) {
                throw new IllegalArgumentException("templateJson.cover.layers must be array");
            }
            if (!root.path("pages").isArray()) {
                throw new IllegalArgumentException("templateJson.pages must be array");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("templateJson is not valid JSON", e);
        }
    }

    private void ensureValidJsonArray(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            if (!node.isArray()) {
                throw new IllegalArgumentException(field + " must be JSON array");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " is not valid JSON", e);
        }
    }

    private void applyReplacements(JsonNode root, java.util.Map<String, String> replacements) {
        // Handle cover layers
        JsonNode coverLayers = root.path("cover").path("layers");
        if (coverLayers.isArray()) {
            for (JsonNode layer : coverLayers) {
                updateLayerIfMatched(layer, replacements);
            }
        }

        // Handle pages layers
        JsonNode pages = root.path("pages");
        if (pages.isArray()) {
            for (JsonNode page : pages) {
                JsonNode pageLayers = page.path("layers");
                if (pageLayers.isArray()) {
                    for (JsonNode layer : pageLayers) {
                        updateLayerIfMatched(layer, replacements);
                    }
                }
            }
        }
    }

    private void updateLayerIfMatched(JsonNode layer, java.util.Map<String, String> replacements) {
        String id = layer.path("id").asText();
        if (replacements.containsKey(id)) {
            String newUrl = replacements.get(id);
            if (layer instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                com.fasterxml.jackson.databind.node.ObjectNode objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) layer;
                objectNode.put("imageUrl", newUrl);
                objectNode.put("originalUrl", newUrl);
                objectNode.put("previewUrl", newUrl);

                // Also check payload for image type layers
                if (layer.has("payload")) {
                    JsonNode payload = layer.get("payload");
                    if (payload instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).put("url", newUrl);
                    }
                }
            }
        }
    }

    private TemplateResponse convertToResponse(TemplateEntity entity, boolean isLiked) {
        List<String> previewImages = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        try {
            if (entity.getPreviewImagesJson() != null) {
                JsonNode imagesNode = objectMapper.readTree(entity.getPreviewImagesJson());
                if (imagesNode.isArray()) {
                    for (JsonNode node : imagesNode) {
                        previewImages.add(node.asText());
                    }
                }
            }
            if (entity.getTagsJson() != null) {
                JsonNode tagsNode = objectMapper.readTree(entity.getTagsJson());
                if (tagsNode.isArray()) {
                    for (JsonNode node : tagsNode) {
                        tags.add(node.asText());
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // Log error or ignore
        }

        boolean isNew = entity.getNewUntil() != null && entity.getNewUntil().isAfter(LocalDateTime.now());
        return TemplateResponse.from(entity, previewImages, tags, isLiked, isNew);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "여행";
        }
        return category.trim();
    }

    private boolean isActive(TemplateEntity entity) {
        return entity.getActive() == null || entity.getActive();
    }

    private int compareTemplateRanking(TemplateEntity a, TemplateEntity b) {
        Comparator<TemplateEntity> comparator = Comparator
                .comparing((TemplateEntity t) -> t.getWeeklyScore() == null ? 0 : t.getWeeklyScore())
                .thenComparing(TemplateEntity::getLikeCount)
                .thenComparing(TemplateEntity::getUserCount)
                .thenComparing(TemplateEntity::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(TemplateEntity::getId, Comparator.nullsLast(Long::compareTo));
        return comparator.reversed().compare(a, b);
    }
}
