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

import java.util.ArrayList;
import java.util.List;
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
            java.util.Map<String, String> replacements) {
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

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
        try {
            if (entity.getPreviewImagesJson() != null) {
                JsonNode imagesNode = objectMapper.readTree(entity.getPreviewImagesJson());
                if (imagesNode.isArray()) {
                    for (JsonNode node : imagesNode) {
                        previewImages.add(node.asText());
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // Log error or ignore
        }

        return TemplateResponse.from(entity, previewImages, isLiked);
    }
}
