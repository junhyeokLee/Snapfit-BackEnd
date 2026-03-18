package com.snapfit.snapfitbackend.global.init;

import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateDataLoader implements CommandLineRunner {

    private final TemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        upsertSeedTemplates();
    }

    private void upsertSeedTemplates() {
        List<TemplateEntity> current = templateRepository.findAll();
        Map<String, TemplateEntity> byTitle = new HashMap<>();
        for (TemplateEntity item : current) {
            byTitle.put(item.getTitle(), item);
        }

        List<TemplateEntity> toSave = new ArrayList<>();
        int created = 0;
        int updated = 0;

        for (TemplateEntity seed : buildSeedTemplates()) {
            TemplateEntity existing = byTitle.get(seed.getTitle());
            if (existing == null) {
                toSave.add(seed);
                created++;
                continue;
            }

            boolean changed = mergeSeedIntoExisting(existing, seed);
            if (changed) {
                toSave.add(existing);
                updated++;
            }
        }

        if (!toSave.isEmpty()) {
            templateRepository.saveAll(toSave);
        }
        log.info("Template seed sync complete. created={}, updated={}, total={}", created, updated, current.size() + created);
    }

    private boolean mergeSeedIntoExisting(TemplateEntity target, TemplateEntity seed) {
        boolean changed = false;

        changed |= setIfDifferent(target.getSubTitle(), seed.getSubTitle(), target::setSubTitle);
        changed |= setIfDifferent(target.getDescription(), seed.getDescription(), target::setDescription);
        changed |= setIfDifferent(target.getCoverImageUrl(), seed.getCoverImageUrl(), target::setCoverImageUrl);
        changed |= setIfDifferent(target.getPreviewImagesJson(), seed.getPreviewImagesJson(), target::setPreviewImagesJson);
        changed |= setIfDifferent(target.getPageCount(), seed.getPageCount(), target::setPageCount);
        changed |= setIfDifferent(target.isBest(), seed.isBest(), target::setBest);
        changed |= setIfDifferent(target.isPremium(), seed.isPremium(), target::setPremium);
        changed |= setIfDifferent(target.getCategory(), seed.getCategory(), target::setCategory);
        changed |= setIfDifferent(target.getTagsJson(), seed.getTagsJson(), target::setTagsJson);
        changed |= setIfDifferent(target.getWeeklyScore(), seed.getWeeklyScore(), target::setWeeklyScore);
        changed |= setIfDifferent(target.getNewUntil(), seed.getNewUntil(), target::setNewUntil);
        changed |= setIfDifferent(target.getTemplateJson(), seed.getTemplateJson(), target::setTemplateJson);

        if (target.getActive() == null || !target.getActive()) {
            target.setActive(true);
            changed = true;
        }

        // Keep service metrics monotonic.
        int mergedLikeCount = Math.max(target.getLikeCount(), seed.getLikeCount());
        if (mergedLikeCount != target.getLikeCount()) {
            target.setLikeCount(mergedLikeCount);
            changed = true;
        }
        int mergedUserCount = Math.max(target.getUserCount(), seed.getUserCount());
        if (mergedUserCount != target.getUserCount()) {
            target.setUserCount(mergedUserCount);
            changed = true;
        }

        return changed;
    }

    private <T> boolean setIfDifferent(T current, T next, java.util.function.Consumer<T> setter) {
        if (current == null && next == null) return false;
        if (current != null && current.equals(next)) return false;
        setter.accept(next);
        return true;
    }

    private boolean setIfDifferent(int current, int next, java.util.function.IntConsumer setter) {
        if (current == next) return false;
        setter.accept(next);
        return true;
    }

    private boolean setIfDifferent(boolean current, boolean next, java.util.function.Consumer<Boolean> setter) {
        if (current == next) return false;
        setter.accept(next);
        return true;
    }

    private List<TemplateEntity> buildSeedTemplates() {
        LocalDateTime now = LocalDateTime.now();
        List<TemplateEntity> list = new ArrayList<>();

        list.add(buildTemplate(
                "오션 브리즈", "푸른 바다의 감성을 담은 템플릿", "여행 사진이 넓게 보이도록 구성된 트래블 템플릿",
                "여행", 20, 921, 210, true, false, 905, now.plusDays(12), SAMPLE_IMAGES_JEJU, "travel"));

        list.add(buildTemplate(
                "Family Weekend", "가족과 보낸 따뜻한 주말", "인물 중심 2분할/3분할 레이아웃으로 구성",
                "가족", 18, 533, 178, false, true, 810, now.plusDays(5), SAMPLE_IMAGES_FAMILY, "family"));

        list.add(buildTemplate(
                "RING MOMENT", "프러포즈 순간을 특별하게", "강한 대비와 포토카드 레이어로 감정선을 강조",
                "연인", 14, 700, 302, true, true, 930, now.plusDays(3), SAMPLE_IMAGES_WEDDING, "couple"));

        list.add(buildTemplate(
                "빛나는 졸업장", "졸업의 순간을 선명하게", "정돈된 그리드와 제목 캡션 중심 템플릿",
                "졸업", 20, 330, 144, false, false, 750, now.plusDays(8), SAMPLE_IMAGES_SCHOOL, "graduation"));

        list.add(buildTemplate(
                "Retro City Tour", "빈티지 감성의 도시 산책 기록", "필름 프레임과 따뜻한 톤을 강조한 레트로 스타일",
                "레트로", 20, 382, 134, false, false, 720, now.plusDays(5), SAMPLE_IMAGES_RETRO, "retro"));

        list.add(buildTemplate(
                "Minimal Focus", "군더더기 없는 미니멀 구성", "큰 여백과 단일 이미지 중심 타이포 레이아웃",
                "미니멀", 16, 248, 101, false, true, 700, now.plusDays(9), SAMPLE_IMAGES_CAFE, "minimal"));

        list.add(buildTemplate(
                "우리들의 여름 제주", "푸른 바다와 돌담길, 우리의 소중한 기록", "감성 프레임과 타이포 중심 제주 여행 시그니처",
                "여행", 24, 1248, 240, true, true, 980, now.plusDays(10), SAMPLE_IMAGES_JEJU, "travel"));

        list.add(buildTemplate(
                "성수동 카페 투어", "힙한 성수동의 모든 것", "카페/거리 사진에 어울리는 콜라주 구성",
                "여행", 12, 856, 120, false, true, 760, now.plusDays(7), SAMPLE_IMAGES_CAFE, "minimal"));

        list.add(buildTemplate(
                "우리의 결혼 1주년", "가장 행복했던 순간들을 영원히", "따뜻한 배경과 감성 캡션 중심 레이아웃",
                "연인", 36, 210, 500, true, true, 940, null, SAMPLE_IMAGES_WEDDING, "couple"));

        return list;
    }

    private TemplateEntity buildTemplate(
            String title,
            String subTitle,
            String description,
            String category,
            int pageCount,
            int userCount,
            int likeCount,
            boolean isBest,
            boolean isPremium,
            int weeklyScore,
            LocalDateTime newUntil,
            String[] images,
            String style) {
        String templateJson = generateStyleTemplateJson(style, title, pageCount, images);
        String tagsJson = generateTagsJson(category, title, subTitle);

        return TemplateEntity.builder()
                .title(title)
                .subTitle(subTitle)
                .description(description)
                .coverImageUrl(images[0])
                .previewImagesJson(generatePreviewImagesJson(pageCount, images))
                .pageCount(pageCount)
                .likeCount(likeCount)
                .userCount(userCount)
                .isBest(isBest)
                .isPremium(isPremium)
                .category(category)
                .tagsJson(tagsJson)
                .weeklyScore(weeklyScore)
                .newUntil(newUntil)
                .active(true)
                .templateJson(templateJson)
                .build();
    }

    private String generateStyleTemplateJson(String style, String title, int pageCount, String[] imageUrls) {
        return switch (style) {
            case "travel" -> generateStyleBookJson("TRAVEL", "paperWarm", "#FF0F766E", "#FF4B5563", "photoCard", "filmSquare", title, pageCount, imageUrls);
            case "family" -> generateStyleBookJson("FAMILY", "paperWhite", "#FF1D4ED8", "#FF475569", "paperTapeCard", "collageTile", title, pageCount, imageUrls);
            case "couple" -> generateStyleBookJson("COUPLE", "paperBeige", "#FFBE185D", "#FF6B7280", "polaroidClassic", "posterPolaroid", title, pageCount, imageUrls);
            case "graduation" -> generateStyleBookJson("GRADUATION", "skyBlue", "#FF1E40AF", "#FF64748B", "collageTile", "filmSquare", title, pageCount, imageUrls);
            case "retro" -> generateStyleBookJson("RETRO", "retroDark", "#FFFFE082", "#FFE5E7EB", "roughPolaroid", "filmSquare", title, pageCount, imageUrls);
            case "minimal" -> generateStyleBookJson("MINIMAL", "minimalGray", "#FF111827", "#FF6B7280", null, "softGlow", title, pageCount, imageUrls);
            default -> generateStyleBookJson("SNAPFIT", "paperWhite", "#FF111827", "#FF6B7280", "photoCard", "collageTile", title, pageCount, imageUrls);
        };
    }

    private String generateStyleBookJson(
            String styleLabel,
            String background,
            String titleColor,
            String subtitleColor,
            String frameA,
            String frameB,
            String title,
            int pageCount,
            String[] imageUrls) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"ratio\":\"1:1\",");
        sb.append("\"cover\":{\"theme\":\"default\",\"layers\":[");
        sb.append(jsonDecorationLayer("cover_bg", background, 0.0, 0.0, 1.0, 1.0));
        sb.append(",");
        sb.append(jsonImageLayer("cover_img", imageUrls[0], 0.08, 0.16, 0.84, 0.58, frameA));
        sb.append(",");
        sb.append(jsonTextLayer("cover_title", title, 0.10, 0.79, 0.80, 0.10, 30, 700, titleColor, "center"));
        sb.append(",");
        sb.append(jsonTextLayer("cover_sub", styleLabel + " TEMPLATE", 0.18, 0.90, 0.64, 0.06, 13, 500, subtitleColor, "center"));
        sb.append("]},");

        sb.append("\"pages\":[");
        for (int i = 1; i <= pageCount; i++) {
            String image1 = imageUrls[(i - 1) % imageUrls.length];
            String image2 = imageUrls[i % imageUrls.length];
            String image3 = imageUrls[(i + 1) % imageUrls.length];
            sb.append("{\"pageNumber\":").append(i).append(",\"layers\":[");
            sb.append(jsonDecorationLayer("p" + i + "_bg", background, 0.0, 0.0, 1.0, 1.0));
            sb.append(",");

            if (i % 3 == 1) {
                sb.append(jsonTextLayer("p" + i + "_title", styleLabel + " DAY " + i, 0.10, 0.07, 0.80, 0.08, 22, 700, titleColor, "left"));
                sb.append(",");
                sb.append(jsonImageLayer("p" + i + "_main", image1, 0.08, 0.18, 0.84, 0.58, frameA));
                sb.append(",");
                sb.append(jsonTextLayer("p" + i + "_cap", "오늘의 하이라이트를 남겨보세요", 0.12, 0.81, 0.76, 0.08, 14, 400, subtitleColor, "left"));
            } else if (i % 3 == 2) {
                sb.append(jsonImageLayer("p" + i + "_left", image1, 0.08, 0.14, 0.40, 0.56, frameA));
                sb.append(",");
                sb.append(jsonImageLayer("p" + i + "_right", image2, 0.52, 0.14, 0.40, 0.56, frameB));
                sb.append(",");
                sb.append(jsonTextLayer("p" + i + "_cap", "감정이 담긴 문장을 써보세요", 0.12, 0.76, 0.76, 0.08, 15, 500, subtitleColor, "center"));
            } else {
                sb.append(jsonImageLayer("p" + i + "_top", image1, 0.08, 0.13, 0.84, 0.34, frameB));
                sb.append(",");
                sb.append(jsonImageLayer("p" + i + "_bottom_left", image2, 0.08, 0.52, 0.40, 0.32, frameA));
                sb.append(",");
                sb.append(jsonImageLayer("p" + i + "_bottom_right", image3, 0.52, 0.52, 0.40, 0.32, frameA));
                sb.append(",");
                sb.append(jsonTextLayer("p" + i + "_title", styleLabel + " SNAP", 0.12, 0.05, 0.76, 0.06, 17, 600, titleColor, "center"));
            }

            sb.append("]}");
            if (i < pageCount) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String jsonDecorationLayer(String id, String background, double x, double y, double w, double h) {
        return "{ \"id\": \"" + id + "\", \"type\": \"DECORATION\", " +
                "\"x\": " + x + ", \"y\": " + y + ", \"width\": " + w + ", \"height\": " + h + ", " +
                "\"rotation\": 0, \"scale\": 1.0, \"payload\": { \"imageBackground\": \"" + background + "\" } }";
    }

    private String jsonImageLayer(String id, String url, double x, double y, double w, double h, String frame) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"id\": \"").append(id).append("\", \"type\": \"IMAGE\", ");
        sb.append("\"x\": ").append(x).append(", \"y\": ").append(y).append(", ");
        sb.append("\"width\": ").append(w).append(", \"height\": ").append(h).append(", ");
        sb.append("\"rotation\": 0, \"scale\": 1.0, ");
        sb.append("\"payload\": { ");
        if (frame != null && !frame.isBlank()) {
            sb.append("\"imageBackground\": \"").append(frame).append("\", ");
        }
        sb.append("\"imageUrl\": \"").append(url).append("\", ");
        sb.append("\"previewUrl\": \"").append(url).append("\", ");
        sb.append("\"originalUrl\": \"").append(url).append("\" ");
        sb.append("} }");
        return sb.toString();
    }

    private String jsonTextLayer(String id, String text, double x, double y, double w, double h,
                                 int fontSize, int fontWeight, String colorHex, String align) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"id\": \"").append(id).append("\", \"type\": \"TEXT\", ");
        sb.append("\"x\": ").append(x).append(", \"y\": ").append(y).append(", ");
        sb.append("\"width\": ").append(w).append(", \"height\": ").append(h).append(", ");
        sb.append("\"rotation\": 0, \"scale\": 1.0, ");
        sb.append("\"payload\": { ");
        sb.append("\"text\": \"").append(escapeJson(text)).append("\", ");
        sb.append("\"textAlign\": \"").append(align).append("\", ");
        sb.append("\"textStyle\": { ");
        sb.append("\"fontSize\": ").append(fontSize).append(", ");
        sb.append("\"fontWeight\": ").append(Math.max(1, fontWeight / 100 - 1)).append(", ");
        sb.append("\"color\": \"").append(colorHex).append("\"");
        sb.append("} } }");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String generatePreviewImagesJson(int count, String[] imageUrls) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < count; i++) {
            sb.append("\"").append(imageUrls[i % imageUrls.length]).append("\"");
            if (i < count - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateTagsJson(String category, String title, String subTitle) {
        String safeCategory = category == null ? "여행" : category;
        String safeTitle = title == null ? "" : title.replace("\n", " ").trim();
        String safeSub = subTitle == null ? "" : subTitle.replace("\n", " ").trim();
        return "[\"" + safeCategory + "\",\"" + safeTitle + "\",\"" + safeSub + "\"]";
    }

    private static final String[] SAMPLE_IMAGES_JEJU = {
            "https://images.unsplash.com/photo-1548115184-bc6544d06a58?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1572099606223-6e29045d7de3?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1528127269322-539801943592?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1498482624419-722a46c10eb3?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80"
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

    private static final String[] SAMPLE_IMAGES_RETRO = {
            "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1458045987530-6d4d4f50c572?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1493244040629-496f6d136cc3?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80"
    };

    private static final String[] SAMPLE_IMAGES_SCHOOL = {
            "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1503676382389-4809596d5290?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80"
    };

    private static final String[] SAMPLE_IMAGES_FAMILY = {
            "https://images.unsplash.com/photo-1511895426328-dc8714191300?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1514090458221-65bb69cf63e6?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1537368910025-700350fe46c7?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80",
            "https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?ixlib=rb-4.0.3&auto=format&fit=crop&w=1740&q=80"
    };
}
