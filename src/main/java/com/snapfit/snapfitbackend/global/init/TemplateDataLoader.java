package com.snapfit.snapfitbackend.global.init;

import com.snapfit.snapfitbackend.domain.template.entity.TemplateEntity;
import com.snapfit.snapfitbackend.domain.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

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
    @Value("${snapfit.template.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Template seed sync skipped (snapfit.template.seed.enabled=false)");
            return;
        }
        upsertSeedTemplates();
    }

    private void upsertSeedTemplates() {
        List<TemplateEntity> current = templateRepository.findAll();
        Map<String, TemplateEntity> byTitle = new HashMap<>();
        for (TemplateEntity item : current) {
            byTitle.put(item.getTitle(), item);
        }

        List<TemplateEntity> seedTemplates = buildSeedTemplates();
        List<TemplateEntity> toSave = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int deactivated = deactivateRemovedTemplates(current, toSave);

        for (TemplateEntity seed : seedTemplates) {
            if (isRemovedTemplateTitle(seed.getTitle())) {
                continue;
            }
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
        log.info("Template seed sync complete. created={}, updated={}, deactivated={}, total={}", created, updated, deactivated, current.size() + created);
    }

    private int deactivateRemovedTemplates(List<TemplateEntity> current, List<TemplateEntity> toSave) {
        int deactivated = 0;
        for (TemplateEntity item : current) {
            boolean removedByRule = isRemovedTemplateTitle(item.getTitle());
            if (!removedByRule) continue;
            if (Boolean.FALSE.equals(item.getActive())) {
                continue;
            }
            item.setActive(false);
            toSave.add(item);
            deactivated++;
        }
        return deactivated;
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
                "SAVE THE DATE",
                "결혼을 알리는 가장 클래식한 세이브 더 데이트 무드",
                "커버 포함 20페이지, 웨딩 포토와 타이포 중심의 에디토리얼 템플릿",
                "웨딩", 20, 1240, 312, true, true, 980, now.plusDays(14), SAMPLE_IMAGES_SIGNATURE_WEDDING, "signature_wedding"));

        list.add(buildTemplate(
                "제주의 기록",
                "제주 여행의 색감과 공기를 담은 트래블 템플릿",
                "커버 포함 20페이지, 여행 사진 중심의 여백감 있는 기록형 템플릿",
                "여행", 20, 1030, 266, true, true, 960, now.plusDays(14), SAMPLE_IMAGES_SUMMER_POSTER, "travel"));

        list.add(buildTemplate(
                "가족의 주말",
                "도시와 일상 속 가족의 순간을 담는 패밀리 템플릿",
                "커버 포함 20페이지, 가족 사진과 메모를 함께 담는 따뜻한 레이아웃",
                "가족", 20, 1105, 294, true, true, 970, now.plusDays(14), SAMPLE_IMAGES_KIDS_BOOKCLUB, "family"));

        list.add(buildTemplate(
                "우리의 기념일",
                "기념일의 감정을 담은 커플 에디토리얼 템플릿",
                "커버 포함 20페이지, 커플 사진과 짧은 문장을 섬세하게 배치한 구성",
                "커플", 20, 980, 241, false, true, 930, now.plusDays(14), SAMPLE_IMAGES_SIGNATURE_WEDDING, "couple"));

        list.add(buildTemplate(
                "Wedding Editorial",
                "화보처럼 정제된 웨딩 무드를 담는 에디토리얼 템플릿",
                "커버 포함 20페이지, 큰 이미지와 절제된 타이포가 중심인 프리미엄 구성",
                "웨딩", 20, 1180, 305, true, true, 975, now.plusDays(14), SAMPLE_IMAGES_SIGNATURE_WEDDING, "signature_wedding"));

        list.add(buildTemplate(
                "Scrapbook",
                "메모와 사진을 자유롭게 엮는 스크랩북 스타일 템플릿",
                "커버 포함 20페이지, 손으로 붙인 듯한 콜라주 감성의 기록형 템플릿",
                "라이프", 20, 960, 228, false, true, 920, now.plusDays(14), SAMPLE_IMAGES_SUMMER_POSTER, "minimal"));

        return list;
    }

    private boolean isRemovedTemplateTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return false;
        }
        String normalized = normalizeTitle(rawTitle);
        List<String> blocked = List.of(
                "브랜드이벤트커버",
                "스카이레더",
                "스카이레터",
                "썸머포스터",
                "웨딩에디토리얼",
                "우리의결혼1주년",
                "성수동카페투어",
                "미니멀포커스",
                "minimalfocus",
                "링모먼트",
                "ringmoment",
                "링트립스토리",
                "오션브리즈",
                "빛나는졸업장",
                "retrocitytour",
                "우리들의여름제주",
                "여름포스터커버",
                "어린이북클럽",
                "시그니처웨딩"
        );
        for (String keyword : blocked) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTitle(String raw) {
        return raw
                .toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^0-9a-z가-힣]", "");
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
            case "summer_poster" -> generateStyleBookJson("SUMMER POSTER", "paperYellow", "#FF0F172A", "#FF334155", "softGlow", "photoCard", title, pageCount, imageUrls);
            case "kids_club" -> generateStyleBookJson("KIDS BOOK CLUB", "darkVignette", "#FFE2E8F0", "#FFCBD5E1", "paperClipCard", "collageTile", title, pageCount, imageUrls);
            case "signature_wedding" -> generateStyleBookJson("SIGNATURE WEDDING", "paperGray", "#FFFFFFFF", "#FFE5E7EB", "softGlow", "photoCard", title, pageCount, imageUrls);
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
        boolean isSummerPoster = "SUMMER POSTER".equals(styleLabel);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"ratio\":\"1:1\",");
        sb.append("\"cover\":{\"theme\":\"default\",\"layers\":[");
        if (isSummerPoster) {
            sb.append(jsonDecorationLayer("cover_top_band", "paperYellow", -0.03, 0.00, 1.06, 0.12));
            sb.append(",");
            sb.append(jsonImageLayer("cover_main_photo", imageUrls[0], -0.01, 0.11, 1.02, 0.78, "free"));
            sb.append(",");
            sb.append(jsonDecorationLayer("cover_bottom_band", "paperYellow", -0.03, 0.89, 1.06, 0.11));
            sb.append(",");
            sb.append(jsonTextLayer("cover_title", "SUMMER", 0.06, 0.01, 0.88, 0.10, 48, 900, "#FF34495E", "left"));
            sb.append(",");
            sb.append(jsonTextLayer("cover_sub", "SNAPKIM 1ST ART EXHIBITION", 0.16, 0.93, 0.68, 0.05, 12, 700, "#FF111827", "center"));
        } else {
            sb.append(jsonDecorationLayer("cover_bg", background, 0.0, 0.0, 1.0, 1.0));
            sb.append(",");
            sb.append(jsonImageLayer("cover_img", imageUrls[0], 0.08, 0.16, 0.84, 0.58, frameA));
            sb.append(",");
            sb.append(jsonTextLayer("cover_title", title, 0.10, 0.79, 0.80, 0.10, 30, 700, titleColor, "center"));
            sb.append(",");
            sb.append(jsonTextLayer("cover_sub", styleLabel + " TEMPLATE", 0.18, 0.90, 0.64, 0.06, 13, 500, subtitleColor, "center"));
        }
        sb.append("]},");

        sb.append("\"pages\":[");
        for (int i = 1; i <= pageCount; i++) {
            String image1 = imageUrls[(i - 1) % imageUrls.length];
            String image2 = imageUrls[i % imageUrls.length];
            String image3 = imageUrls[(i + 1) % imageUrls.length];
            sb.append("{\"pageNumber\":").append(i).append(",\"layers\":[");
            sb.append(jsonDecorationLayer("p" + i + "_bg", background, 0.0, 0.0, 1.0, 1.0));
            sb.append(",");

            if (isSummerPoster && i == 1) {
                sb.append(jsonDecorationLayer("p1_top_band", "paperYellow", -0.03, 0.00, 1.06, 0.12));
                sb.append(",");
                sb.append(jsonImageLayer("p1_main_photo", imageUrls[0], -0.01, 0.11, 1.02, 0.78, "free"));
                sb.append(",");
                sb.append(jsonDecorationLayer("p1_bottom_band", "paperYellow", -0.03, 0.89, 1.06, 0.11));
                sb.append(",");
                sb.append(jsonTextLayer("p1_title", "SUMMER", 0.06, 0.01, 0.88, 0.10, 48, 900, "#FF34495E", "left"));
                sb.append(",");
                sb.append(jsonTextLayer("p1_sub", "SNAPKIM 1ST ART EXHIBITION", 0.16, 0.93, 0.68, 0.05, 12, 700, "#FF111827", "center"));
            } else if (i % 3 == 1) {
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

    private static final String[] SAMPLE_IMAGES_SUMMER_POSTER = {
            "http://54.253.3.176/template-assets/figma/moa_frame1_cover.png",
            "https://images.pexels.com/photos/457882/pexels-photo-457882.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/355465/pexels-photo-355465.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/132037/pexels-photo-132037.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1054218/pexels-photo-1054218.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1268855/pexels-photo-1268855.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/210243/pexels-photo-210243.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/145939/pexels-photo-145939.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/417074/pexels-photo-417074.jpeg?auto=compress&cs=tinysrgb&w=1600"
    };

    private static final String[] SAMPLE_IMAGES_KIDS_BOOKCLUB = {
            "https://images.pexels.com/photos/159866/books-book-pages-read-literature-159866.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/46274/pexels-photo-46274.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/256455/pexels-photo-256455.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1761279/pexels-photo-1761279.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1370298/pexels-photo-1370298.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1181671/pexels-photo-1181671.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1648377/pexels-photo-1648377.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/3014856/pexels-photo-3014856.jpeg?auto=compress&cs=tinysrgb&w=1600"
    };

    private static final String[] SAMPLE_IMAGES_SIGNATURE_WEDDING = {
            "https://images.pexels.com/photos/1024993/pexels-photo-1024993.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/265856/pexels-photo-265856.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/169198/pexels-photo-169198.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1024960/pexels-photo-1024960.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/169190/pexels-photo-169190.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1024994/pexels-photo-1024994.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/931162/pexels-photo-931162.jpeg?auto=compress&cs=tinysrgb&w=1600",
            "https://images.pexels.com/photos/1128782/pexels-photo-1128782.jpeg?auto=compress&cs=tinysrgb&w=1600"
    };
}
