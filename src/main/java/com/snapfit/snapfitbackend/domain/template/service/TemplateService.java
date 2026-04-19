package com.snapfit.snapfitbackend.domain.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.service.AlbumService;
import com.snapfit.snapfitbackend.domain.billing.service.BillingService;
import com.snapfit.snapfitbackend.domain.template.dto.response.TemplateResponse;
import com.snapfit.snapfitbackend.domain.template.dto.response.TemplateSummaryPageResponse;
import com.snapfit.snapfitbackend.domain.template.dto.response.TemplateSummaryResponse;
import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import com.snapfit.snapfitbackend.domain.template.entity.TemplateLikeEntity;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateLikeRepository;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateLikeRepository templateLikeRepository;
    private final AlbumService albumService;
    private final ObjectMapper objectMapper;
    private final BillingService billingService;

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
    public TemplateSummaryPageResponse getTemplateSummaries(String userId, int page, int size) {
        final int safePage = Math.max(page, 0);
        final int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Order.desc("weeklyScore"),
                        Sort.Order.desc("likeCount"),
                        Sort.Order.desc("userCount"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")));

        Page<TemplateEntity> templatePage = templateRepository.findAllActive(pageable);
        List<TemplateSummaryResponse> content = templatePage.getContent().stream()
                .map(entity -> {
                    boolean isLiked = false;
                    if (userId != null && !userId.isEmpty()) {
                        isLiked = templateLikeRepository.existsByTemplateIdAndUserId(entity.getId(), userId);
                    }
                    boolean isNew = entity.getNewUntil() != null && entity.getNewUntil().isAfter(LocalDateTime.now());
                    return TemplateSummaryResponse.of(
                            entity.getId(),
                            entity.getTitle(),
                            entity.getCoverImageUrl(),
                            parseJsonArray(entity.getTagsJson()),
                            entity.getWeeklyScore() == null ? 0 : entity.getWeeklyScore(),
                            entity.getLikeCount(),
                            entity.getUserCount(),
                            entity.isPremium(),
                            entity.isBest(),
                            isNew,
                            isLiked);
                })
                .collect(Collectors.toList());

        return TemplateSummaryPageResponse.builder()
                .content(content)
                .page(templatePage.getNumber())
                .size(templatePage.getSize())
                .totalElements(templatePage.getTotalElements())
                .totalPages(templatePage.getTotalPages())
                .hasNext(templatePage.hasNext())
                .build();
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
        if (template.isPremium() && !billingService.hasActiveSubscription(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Premium template requires active subscription");
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
        final boolean isCreate = input.getId() == null;
        TemplateEntity target;
        if (!isCreate) {
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

        if (input.getWeeklyScore() != null) {
            target.setWeeklyScore(Math.max(0, input.getWeeklyScore()));
        } else if (isCreate) {
            // 신규 등록 템플릿은 첫 페이지에서 바로 확인되도록 기본 노출 점수를 부여한다.
            target.setWeeklyScore(500);
        }

        if (input.getNewUntil() != null) {
            target.setNewUntil(input.getNewUntil());
        } else if (isCreate) {
            target.setNewUntil(LocalDateTime.now().plusDays(7));
        }

        target.setActive(input.getActive() == null ? true : input.getActive());
        target.setTemplateJson(input.getTemplateJson());
        return templateRepository.save(target);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateTemplateDraftForAdmin(TemplateEntity input) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (input.getTitle() == null || input.getTitle().trim().isEmpty()) {
            errors.add("title is required");
        }
        if (input.getCoverImageUrl() == null || input.getCoverImageUrl().trim().isEmpty()) {
            errors.add("coverImageUrl is required");
        }
        if (input.getPageCount() <= 0) {
            errors.add("pageCount must be greater than 0");
        } else if (input.getPageCount() < 12 || input.getPageCount() > 24) {
            warnings.add("pageCount recommended range is 12..24");
        }
        if (input.getTemplateJson() == null || input.getTemplateJson().trim().isEmpty()) {
            errors.add("templateJson is required");
        }

        JsonNode root = null;
        if (input.getTemplateJson() != null && !input.getTemplateJson().trim().isEmpty()) {
            try {
                root = objectMapper.readTree(input.getTemplateJson());
            } catch (JsonProcessingException e) {
                errors.add("templateJson is not valid JSON");
            }
        }

        if (root != null) {
            JsonNode cover = root.path("cover");
            if (cover.isMissingNode()) {
                errors.add("templateJson.cover is required");
            } else {
                JsonNode coverLayers = cover.path("layers");
                if (!coverLayers.isArray()) {
                    errors.add("templateJson.cover.layers must be array");
                } else if (coverLayers.isEmpty()) {
                    warnings.add("cover.layers is empty");
                }
            }

            JsonNode pages = root.path("pages");
            if (!pages.isArray()) {
                errors.add("templateJson.pages must be array");
            } else {
                if (pages.isEmpty()) {
                    errors.add("templateJson.pages must not be empty");
                }
                if (input.getPageCount() > 0 && pages.size() != input.getPageCount()) {
                    warnings.add("pageCount(" + input.getPageCount() + ") != pages.length(" + pages.size() + ")");
                }
                for (int i = 0; i < pages.size(); i++) {
                    JsonNode page = pages.get(i);
                    JsonNode layers = page.path("layers");
                    if (!layers.isArray()) {
                        errors.add("pages[" + i + "].layers must be array");
                        continue;
                    }
                    if (layers.isEmpty()) {
                        warnings.add("pages[" + i + "].layers is empty");
                    }
                }
            }
        }

        if (input.getPreviewImagesJson() != null && !input.getPreviewImagesJson().trim().isEmpty()) {
            try {
                JsonNode p = objectMapper.readTree(input.getPreviewImagesJson());
                if (!p.isArray()) {
                    errors.add("previewImagesJson must be JSON array");
                } else if (p.isEmpty()) {
                    warnings.add("previewImagesJson is empty");
                }
            } catch (JsonProcessingException e) {
                errors.add("previewImagesJson is not valid JSON");
            }
        } else {
            warnings.add("previewImagesJson is empty");
        }

        if (input.getTagsJson() != null && !input.getTagsJson().trim().isEmpty()) {
            try {
                JsonNode t = objectMapper.readTree(input.getTagsJson());
                if (!t.isArray()) {
                    errors.add("tagsJson must be JSON array");
                }
            } catch (JsonProcessingException e) {
                errors.add("tagsJson is not valid JSON");
            }
        } else {
            warnings.add("tagsJson is empty");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("errorCount", errors.size());
        result.put("warningCount", warnings.size());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminTemplatePage(int page, int size) {
        final int safePage = Math.max(page, 0);
        final int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
        Page<TemplateEntity> p = templateRepository.findAll(pageable);

        List<Map<String, Object>> items = p.getContent().stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("title", t.getTitle());
            row.put("category", t.getCategory());
            row.put("isPremium", t.isPremium());
            row.put("isBest", t.isBest());
            row.put("active", t.getActive() == null ? true : t.getActive());
            row.put("pageCount", t.getPageCount());
            row.put("coverImageUrl", t.getCoverImageUrl());
            row.put("updatedAt", t.getUpdatedAt());
            row.put("createdAt", t.getCreatedAt());
            return row;
        }).toList();

        return Map.of(
                "items", items,
                "page", p.getNumber(),
                "size", p.getSize(),
                "totalElements", p.getTotalElements(),
                "totalPages", p.getTotalPages(),
                "hasNext", p.hasNext());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminTemplateDetail(Long id) {
        TemplateEntity t = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", t.getId());
        row.put("title", t.getTitle());
        row.put("subTitle", t.getSubTitle());
        row.put("description", t.getDescription());
        row.put("coverImageUrl", t.getCoverImageUrl());
        row.put("previewImagesJson", t.getPreviewImagesJson());
        row.put("pageCount", t.getPageCount());
        row.put("likeCount", t.getLikeCount());
        row.put("userCount", t.getUserCount());
        row.put("isBest", t.isBest());
        row.put("isPremium", t.isPremium());
        row.put("category", t.getCategory());
        row.put("tagsJson", t.getTagsJson());
        row.put("weeklyScore", t.getWeeklyScore());
        row.put("active", t.getActive() == null ? true : t.getActive());
        row.put("templateJson", t.getTemplateJson());
        row.put("updatedAt", t.getUpdatedAt());
        return row;
    }

    @Transactional
    public Map<String, Object> setTemplateActive(Long id, boolean active) {
        TemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        entity.setActive(active);
        TemplateEntity saved = templateRepository.save(entity);
        return Map.of(
                "id", saved.getId(),
                "active", saved.getActive() == null ? true : saved.getActive());
    }

    @Transactional
    public Map<String, Object> deleteTemplateForAdmin(Long id) {
        TemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        long deletedLikes = templateLikeRepository.deleteByTemplateId(id);
        templateRepository.delete(entity);

        return Map.of(
                "id", id,
                "deleted", true,
                "deletedLikes", deletedLikes
        );
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
        List<String> previewImages = parseJsonArray(entity.getPreviewImagesJson());
        List<String> tags = parseJsonArray(entity.getTagsJson());

        boolean isNew = entity.getNewUntil() != null && entity.getNewUntil().isAfter(LocalDateTime.now());
        return TemplateResponse.from(entity, previewImages, tags, isLiked, isNew);
    }

    private List<String> parseJsonArray(String raw) {
        List<String> items = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return items;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isArray()) {
                for (JsonNode child : node) {
                    final String text = child.asText("");
                    if (!text.isBlank()) {
                        items.add(text);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // ignore invalid metadata and keep empty list
        }
        return items;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "여행";
        }
        String normalized = category.trim().toLowerCase();
        return switch (normalized) {
            case "travel" -> "여행";
            case "wedding" -> "웨딩";
            case "kids", "kid" -> "키즈";
            case "poster" -> "포스터";
            case "brand" -> "브랜드";
            case "romance", "romantic" -> "로맨스";
            case "magazine" -> "매거진";
            case "family" -> "가족";
            case "graduation" -> "졸업";
            case "retro" -> "레트로";
            case "general", "default" -> "일반";
            default -> category.trim();
        };
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
