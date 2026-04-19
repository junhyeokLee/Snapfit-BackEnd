package com.snapfit.snapfitbackend.domain.template.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateAiDraftService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${snapfit.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${snapfit.ai.openai.model:gpt-4.1-mini}")
    private String openAiModel;

    @Value("${snapfit.ai.openai.responses-url:https://api.openai.com/v1/responses}")
    private String openAiResponsesUrl;

    public Map<String, Object> generateDraft(String title, String style, String description, Integer pageCount) {
        String safeTitle = title == null ? "" : title.trim();
        if (safeTitle.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        String safeStyle = normalizeStyle(style);
        String safeDescription = description == null ? "" : description.trim();
        int safePageCount = Math.max(12, Math.min(24, pageCount == null ? 12 : pageCount));

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return buildFallbackDraft(safeTitle, safeStyle, safeDescription, safePageCount);
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", openAiModel);
            body.put("input", List.of(
                    Map.of(
                            "role", "system",
                            "content", "You are a template JSON generator for a photo album editor. Return only JSON object with keys: coverImageUrl, previewImages, tags, pageCount, templateJson. templateJson must include ratio='1.0', cover.layers[], pages[]. Keep layers editable and simple."
                    ),
                    Map.of(
                            "role", "user",
                            "content", "title=" + safeTitle + ", style=" + safeStyle + ", pageCount=" + safePageCount +
                                    ", description=" + safeDescription +
                                    ". Use public image URLs (picsum.photos seed based), modern typography, cover + pages tone consistency. Keep JSON compatible with editor."
                    )
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(openAiApiKey.trim());

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    openAiResponsesUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("OpenAI draft generation failed status={}", response.getStatusCode());
                return buildFallbackDraft(safeTitle, safeStyle, safeDescription, safePageCount);
            }

            Map<String, Object> parsed = parseOpenAiResponseToDraft(response.getBody());
            if (parsed == null || parsed.isEmpty()) {
                return buildFallbackDraft(safeTitle, safeStyle, safeDescription, safePageCount);
            }

            sanitizeDraft(parsed, safeTitle, safeStyle, safeDescription, safePageCount);
            return parsed;
        } catch (Exception e) {
            log.warn("OpenAI draft generation exception: {}", e.getMessage());
            return buildFallbackDraft(safeTitle, safeStyle, safeDescription, safePageCount);
        }
    }

    private String normalizeStyle(String style) {
        String s = (style == null ? "" : style.trim().toLowerCase(Locale.ROOT));
        if (s.isBlank()) return "minimal";
        if (List.of("minimal", "romance", "travel", "magazine", "wedding", "kids", "poster", "brand").contains(s)) return s;
        return "minimal";
    }

    private Map<String, Object> parseOpenAiResponseToDraft(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String outputText = root.path("output_text").asText("");
            if (outputText.isBlank()) {
                JsonNode output = root.path("output");
                if (output.isArray()) {
                    for (JsonNode item : output) {
                        JsonNode content = item.path("content");
                        if (!content.isArray()) continue;
                        for (JsonNode c : content) {
                            String txt = c.path("text").asText("");
                            if (!txt.isBlank()) {
                                outputText = txt;
                                break;
                            }
                        }
                        if (!outputText.isBlank()) break;
                    }
                }
            }
            if (outputText.isBlank()) return null;
            String jsonText = extractJsonObject(outputText);
            if (jsonText == null) return null;
            return objectMapper.readValue(jsonText, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("parseOpenAiResponseToDraft failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return text.substring(start, end + 1);
    }

    private void sanitizeDraft(Map<String, Object> draft, String title, String style, String description, int pageCount) {
        draft.put("title", title);
        draft.put("pageCount", pageCount);
        draft.put("style", style);
        draft.put("description", description);
        draft.put("category", categoryFromStyle(style));

        Object preview = draft.get("previewImages");
        if (!(preview instanceof List<?> list) || list.isEmpty()) {
            String seed = Integer.toString(Math.abs((title + "-" + style).hashCode()), 36);
            draft.put("previewImages", List.of(
                    "https://picsum.photos/seed/" + seed + "1/1200/1600",
                    "https://picsum.photos/seed/" + seed + "2/1200/1600",
                    "https://picsum.photos/seed/" + seed + "3/1200/1600",
                    "https://picsum.photos/seed/" + seed + "4/1200/1600"
            ));
        }

        if (!(draft.get("tags") instanceof List<?>)) {
            draft.put("tags", List.of(style, "ai-generated", "admin-quick"));
        }

        Object templateJsonObj = draft.get("templateJson");
        if (templateJsonObj instanceof String text) {
            try {
                draft.put("templateJson", objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {}));
            } catch (Exception ignored) {
                draft.put("templateJson", buildFallbackDraft(title, style, description, pageCount).get("templateJson"));
            }
        }
        if (!(draft.get("templateJson") instanceof Map<?, ?>)) {
            draft.put("templateJson", buildFallbackDraft(title, style, description, pageCount).get("templateJson"));
        }
        draft.put("source", "openai");
    }

    private Map<String, Object> buildFallbackDraft(String title, String style, String description, int pageCount) {
        String seed = Integer.toString(Math.abs((title + "-" + style).hashCode()), 36);
        String cover = "https://picsum.photos/seed/" + seed + "/1200/1600";
        String subtitle = (description == null || description.isBlank()) ? "SNAPFIT TEMPLATE" : description.trim();

        List<Map<String, Object>> coverLayers = new ArrayList<>();
        coverLayers.add(decoration("cover_top_bg", -0.02, 0.00, 1.04, 0.16, "paperYellow", 1));
        coverLayers.add(image("cover_main_photo", -0.02, 0.16, 1.04, 0.75, cover, 2));
        coverLayers.add(decoration("cover_bottom_bg", -0.02, 0.91, 1.04, 0.09, "paperYellow", 15));
        coverLayers.add(text("cover_title", title.toUpperCase(Locale.ROOT), 0.05, 0.03, 0.88, 0.10, "#ff1e2a36", 34, 8, "left", 10));
        coverLayers.add(text("cover_subtitle", subtitle, 0.20, 0.935, 0.70, 0.04, "#ff111111", 12, 7, "center", 16));

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = 1; i <= pageCount; i++) {
            String image = "https://picsum.photos/seed/" + seed + "_" + i + "/1400/1600";
            pages.add(Map.of(
                    "pageNumber", i,
                    "layers", List.of(
                            image("p" + i + "_photo", -0.01, 0.00, 1.02, 1.0, image, 2),
                            text("p" + i + "_title", title + " " + i, 0.06, 0.06, 0.82, 0.08, "#ffffffff", 26, 8, "left", 10),
                            text("p" + i + "_sub", subtitle, 0.06, 0.14, 0.60, 0.04, "#ffeef5ff", 14, 6, "left", 11)
                    )
            ));
        }

        Map<String, Object> templateJson = new LinkedHashMap<>();
        templateJson.put("templateId", "ai_" + seed);
        templateJson.put("name", title);
        templateJson.put("ratio", "1.0");
        templateJson.put("cover", Map.of("theme", "auto", "layers", coverLayers));
        templateJson.put("pages", pages);
        templateJson.put("metadata", Map.of("style", style, "applyScope", "cover_and_pages"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", title);
        result.put("style", style);
        result.put("description", subtitle);
        result.put("category", categoryFromStyle(style));
        result.put("pageCount", pageCount);
        result.put("coverImageUrl", cover);
        result.put("previewImages", List.of(
                "https://picsum.photos/seed/" + seed + "1/1200/1600",
                "https://picsum.photos/seed/" + seed + "2/1200/1600",
                "https://picsum.photos/seed/" + seed + "3/1200/1600",
                "https://picsum.photos/seed/" + seed + "4/1200/1600"
        ));
        result.put("tags", List.of(style, "ai-generated", "admin-quick"));
        result.put("templateJson", templateJson);
        result.put("source", "fallback");
        return result;
    }

    private String categoryFromStyle(String style) {
        return switch ((style == null ? "" : style.trim().toLowerCase(Locale.ROOT))) {
            case "travel" -> "여행";
            case "wedding" -> "웨딩";
            case "kids" -> "키즈";
            case "poster" -> "포스터";
            case "brand" -> "브랜드";
            case "romance" -> "로맨스";
            case "magazine" -> "매거진";
            default -> "일반";
        };
    }

    private Map<String, Object> image(String id, double x, double y, double width, double height, String url, int z) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("imageBackground", "free");
        payload.put("imageTemplate", null);
        payload.put("originalUrl", url);
        payload.put("previewUrl", url);
        payload.put("imageUrl", url);

        Map<String, Object> layer = baseLayer(id, "IMAGE", x, y, width, height, z);
        layer.put("payload", payload);
        return layer;
    }

    private Map<String, Object> text(String id, String content, double x, double y, double width, double height,
                                     String color, int fontSize, int weight, String align, int z) {
        Map<String, Object> style = new LinkedHashMap<>();
        style.put("fontSize", fontSize);
        style.put("fontSizeRatio", 0.05);
        style.put("fontWeight", weight);
        style.put("fontStyle", 0);
        style.put("fontFamily", "Inter");
        style.put("color", color);
        style.put("letterSpacing", 0.0);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", content);
        payload.put("textAlign", align);
        payload.put("textStyleType", "none");
        payload.put("textBackground", null);
        payload.put("bubbleColor", null);
        payload.put("textStyle", style);

        Map<String, Object> layer = baseLayer(id, "TEXT", x, y, width, height, z);
        layer.put("payload", payload);
        return layer;
    }

    private Map<String, Object> decoration(String id, double x, double y, double width, double height,
                                           String colorKey, int z) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("imageBackground", colorKey);
        payload.put("imageTemplate", "free");
        payload.put("originalUrl", null);
        payload.put("previewUrl", null);
        payload.put("imageUrl", null);

        Map<String, Object> layer = baseLayer(id, "DECORATION", x, y, width, height, z);
        layer.put("payload", payload);
        return layer;
    }

    private Map<String, Object> baseLayer(String id, String type, double x, double y, double width, double height, int z) {
        Map<String, Object> layer = new LinkedHashMap<>();
        layer.put("id", id);
        layer.put("type", type);
        layer.put("zIndex", z);
        layer.put("x", x);
        layer.put("y", y);
        layer.put("width", width);
        layer.put("height", height);
        layer.put("scale", 1.0);
        layer.put("rotation", 0.0);
        layer.put("opacity", 1.0);
        return layer;
    }
}
