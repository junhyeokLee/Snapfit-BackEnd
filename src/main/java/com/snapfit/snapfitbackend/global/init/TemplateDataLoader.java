package com.snapfit.snapfitbackend.global.init;

import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateDataLoader implements CommandLineRunner {

        private final TemplateRepository templateRepository;

        @Override
        public void run(String... args) throws Exception {
                if (templateRepository.count() == 0) {
                        saveInitialTemplates();
                } else {
                        updateExistingTemplates();
                }
        }

        private void saveInitialTemplates() {
                TemplateEntity t1 = TemplateEntity.builder()
                                .title("우리들의\n여름 제주")
                                .subTitle("푸른 바다와 돌담길, 우리의 소중한 기록을 감성적인 레이아웃에 담아보세요.")
                                .description("제주 여행의 감성을 담은 프리미엄 템플릿입니다.")
                                .coverImageUrl(
                                                "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80")
                                .pageCount(24)
                                .userCount(1248)
                                .likeCount(240)
                                .isBest(true)
                                .isPremium(true)
                                .templateJson(generateTemplateJson(24, SAMPLE_IMAGES_JEJU))
                                .previewImagesJson(generatePreviewImagesJson(24, SAMPLE_IMAGES_JEJU))
                                .build();

                TemplateEntity t2 = TemplateEntity.builder()
                                .title("성수동\n카페 투어")
                                .subTitle("힙한 성수동의 모든 것")
                                .description("감각적인 카페 사진을 위한 레이아웃.")
                                .coverImageUrl(
                                                "https://images.unsplash.com/photo-1521017432531-fbd92d768814?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80")
                                .pageCount(12)
                                .userCount(856)
                                .likeCount(120)
                                .isBest(false)
                                .isPremium(true)
                                .templateJson(generateTemplateJson(12, SAMPLE_IMAGES_CAFE))
                                .previewImagesJson(generatePreviewImagesJson(12, SAMPLE_IMAGES_CAFE))
                                .build();

                TemplateEntity t3 = TemplateEntity.builder()
                                .title("우리의\n결혼 1주년")
                                .subTitle("가장 행복했던 순간들을 영원히")
                                .description("로맨틱한 분위기의 웨딩 앨범 템플릿.")
                                .coverImageUrl(
                                                "https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80")
                                .pageCount(36)
                                .userCount(210)
                                .likeCount(500)
                                .isBest(true)
                                .isPremium(true)
                                .templateJson(generateTemplateJson(36, SAMPLE_IMAGES_WEDDING))
                                .previewImagesJson(generatePreviewImagesJson(36, SAMPLE_IMAGES_WEDDING))
                                .build();

                templateRepository.saveAll(List.of(t1, t2, t3));
                log.info("Initialized 3 templates with realistic data.");
        }

        private void updateExistingTemplates() {
                List<TemplateEntity> templates = templateRepository.findAll();
                boolean updated = false;
                for (TemplateEntity t : templates) {
                        boolean needsUpdate = false;
                        // Simply overwrite to ensure we have the new images
                        String[] images = SAMPLE_IMAGES_JEJU; // Default
                        if (t.getTitle().contains("성수동"))
                                images = SAMPLE_IMAGES_CAFE;
                        if (t.getTitle().contains("결혼"))
                                images = SAMPLE_IMAGES_WEDDING;

                        // Force update for Jeju template to apply new layout
                        if (t.getTitle().contains("제주")) {
                                t.setTemplateJson(generateJejuTemplateJson(t.getPageCount(), images));
                                needsUpdate = true;
                        } else if (t.getTemplateJson() == null || "{}".equals(t.getTemplateJson())
                                        || !t.getTemplateJson().contains("img_")) {
                                t.setTemplateJson(generateTemplateJson(t.getPageCount(), images));
                                needsUpdate = true;
                        }

                        // Update preview images if missing
                        if (t.getPreviewImagesJson() == null || "[]".equals(t.getPreviewImagesJson())
                                        || t.getPreviewImagesJson().length() < 100) {
                                t.setPreviewImagesJson(generatePreviewImagesJson(t.getPageCount(), images));
                                needsUpdate = true;
                        }

                        if (needsUpdate) {
                                updated = true;
                        }
                }

                if (updated) {
                        templateRepository.saveAll(templates);
                        log.info("Updated existing templates with realistic data.");
                }
        }

        // --- Customized Generator for "Our Summer Jeju" ---
        private String generateJejuTemplateJson(int pageCount, String[] imageUrls) {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"ratio\": \"1:1\",");

                // Cover: Full Image + Big Title + Subtitle
                String coverImg = imageUrls[0];
                sb.append("\"cover\": { \"theme\": \"default\", \"layers\": [");
                sb.append(jsonImageLayer("img_cover", coverImg, 0.0, 0.0, 1.0, 1.0, null));
                sb.append(",");
                sb.append(jsonTextLayer("txt_title", "JEJU\nSUMMER", 0.1, 0.35, 0.8, 0.3, 60, 900, "#FFFFFFFF",
                                "center")); // Bold White
                sb.append(",");
                sb.append(jsonTextLayer("txt_sub", "2024.08.15 - 08.18", 0.1, 0.65, 0.8, 0.1, 18, 500, "#DDFFFFFF",
                                "center"));
                sb.append("] },");

                sb.append("\"pages\": [");
                for (int i = 1; i <= pageCount; i++) {
                        String imageUrl = imageUrls[(i - 1) % imageUrls.length];
                        sb.append("{ \"pageNumber\": ").append(i).append(", \"layers\": [");

                        // Vary layouts based on page number
                        if (i == 1) {
                                // Page 1: Intro / Table of Contents style
                                // Text on top, Image bottom
                                sb.append(jsonTextLayer("p" + i + "_t1", "Travel Log", 0.1, 0.1, 0.8, 0.1, 24, 700,
                                                "#FF333333", "center"));
                                sb.append(",");
                                sb.append(jsonTextLayer("p" + i + "_t2", "우리의 소중한 기억들", 0.1, 0.2, 0.8, 0.05, 14, 400,
                                                "#FF666666", "center"));
                                sb.append(",");
                                sb.append(jsonImageLayer("p" + i + "_img", imageUrl, 0.1, 0.35, 0.8, 0.55, "shadow"));

                        } else if (i % 3 == 0) {
                                // Polaroid Style with Tape
                                sb.append(jsonImageLayer("p" + i + "_img", imageUrl, 0.15, 0.15, 0.7, 0.6, "polaroid"));
                                sb.append(",");
                                // Handwritten caption below polaroid
                                sb.append(jsonTextLayer("p" + i + "_txt", "Happy Moment", 0.2, 0.8, 0.6, 0.1, 16, 400,
                                                "#FF555555", "center"));

                        } else if (i % 3 == 1) {
                                // Full Screen Bleed (Scenic)
                                sb.append(jsonImageLayer("p" + i + "_img", imageUrl, 0.0, 0.0, 1.0, 1.0, null));
                                sb.append(",");
                                // Small overlay text at bottom right
                                sb.append(jsonTextLayer("p" + i + "_date", "2024.08.16", 0.6, 0.9, 0.35, 0.05, 12, 400,
                                                "#CCFFFFFF", "right"));

                        } else {
                                // Clean Frame: Image with white border margin
                                sb.append(jsonImageLayer("p" + i + "_img", imageUrl, 0.05, 0.05, 0.9, 0.9, "shadow"));
                        }

                        sb.append("] }");
                        if (i < pageCount)
                                sb.append(",");
                }
                sb.append("]");
                sb.append("}");
                return sb.toString();
        }

        // --- Helper Methods to generate JSON strings ---

        private String jsonImageLayer(String id, String url, double x, double y, double w, double h, String frame) {
                StringBuilder sb = new StringBuilder();
                sb.append("{ \"id\": \"").append(id).append("\", \"type\": \"IMAGE\", ");
                sb.append("\"x\": ").append(x).append(", \"y\": ").append(y).append(", ");
                sb.append("\"width\": ").append(w).append(", \"height\": ").append(h).append(", ");
                sb.append("\"rotation\": 0, \"scale\": 1.0, ");
                sb.append("\"payload\": { ");
                if (frame != null)
                        sb.append("\"imageBackground\": \"").append(frame).append("\", ");
                sb.append("\"imageUrl\": \"").append(url).append("\", ");
                sb.append("\"previewUrl\": \"").append(url).append("\", ");
                sb.append("\"originalUrl\": \"").append(url).append("\" ");
                sb.append("} }");
                return sb.toString();
        }

        private String jsonTextLayer(String id, String text, double x, double y, double w, double h, int fontSize,
                        int fontWeight, String colorHex, String align) {
                StringBuilder sb = new StringBuilder();
                sb.append("{ \"id\": \"").append(id).append("\", \"type\": \"TEXT\", ");
                sb.append("\"x\": ").append(x).append(", \"y\": ").append(y).append(", ");
                sb.append("\"width\": ").append(w).append(", \"height\": ").append(h).append(", ");
                sb.append("\"rotation\": 0, \"scale\": 1.0, ");
                sb.append("\"payload\": { ");
                sb.append("\"text\": \"").append(text.replace("\n", "\\n")).append("\", ");
                sb.append("\"textAlign\": \"").append(align).append("\", ");
                sb.append("\"textStyle\": { ");
                sb.append("\"fontSize\": ").append(fontSize).append(", ");
                sb.append("\"fontWeight\": ").append(fontWeight / 100 - 1).append(", "); // 400->3 (w400 is index 3 in
                                                                                         // list?) Check mapping.
                // LayerExportMapper: _fontWeights = [w100, w200, w300, w400(3), w500(4),
                // w600(5), w700(6)...]
                // Let's pass raw index if we can, or just simplify.
                // Actually helper: 400->3, 700->6.
                // int weightIdx = (fontWeight / 100) - 1;
                // Let's rely on standard logic.
                sb.append("\"color\": \"").append(colorHex).append("\"");
                sb.append("} } }");
                return sb.toString();
        }

        private String generateTemplateJson(int pageCount, String[] imageUrls) {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"ratio\": \"1:1\",");
                sb.append("\"cover\": { \"theme\": \"default\", \"layers\": [] },");
                sb.append("\"pages\": [");
                for (int i = 1; i <= pageCount; i++) {
                        String imageUrl = imageUrls[(i - 1) % imageUrls.length];
                        sb.append("{ \"pageNumber\": ").append(i).append(", \"layers\": [");
                        // Add a full-screen image layer
                        sb.append("{ \"type\": \"IMAGE\", \"id\": \"img_").append(i).append(
                                        "\", \"x\": 0.0, \"y\": 0.0, \"width\": 1.0, \"height\": 1.0, \"rotation\": 0, \"scale\": 1.0, \"payload\": { \"imageUrl\": \"")
                                        .append(imageUrl).append("\", \"originalUrl\": \"").append(imageUrl)
                                        .append("\" } }");
                        sb.append("] }");
                        if (i < pageCount)
                                sb.append(",");
                }
                sb.append("]");
                sb.append("}");
                return sb.toString();
        }

        private String generatePreviewImagesJson(int count, String[] imageUrls) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (int i = 0; i < count; i++) {
                        sb.append("\"").append(imageUrls[i % imageUrls.length]).append("\"");
                        if (i < count - 1)
                                sb.append(",");
                }
                sb.append("]");
                return sb.toString();
        }

        // Sample images for realistic feel
        private static final String[] SAMPLE_IMAGES_JEJU = {
                        "https://images.unsplash.com/photo-1548115184-bc6544d06a58?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1572099606223-6e29045d7de3?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1528127269322-539801943592?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1498482624419-722a46c10eb3?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1551632811-561732d1e306?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80"
        };

        private static final String[] SAMPLE_IMAGES_CAFE = {
                        "https://images.unsplash.com/photo-1554118811-1e0d58224f24?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1509042239860-f550ce710b93?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80"
        };

        private static final String[] SAMPLE_IMAGES_WEDDING = {
                        "https://images.unsplash.com/photo-1511285560982-1356c11d4606?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1519741497674-611481863552?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
                        "https://images.unsplash.com/photo-1522673607200-1645062cd958?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80"
        };
}
