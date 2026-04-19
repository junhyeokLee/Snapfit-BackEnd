package com.snapfit.snapfitbackend.domain.order.service;

import com.snapfit.snapfitbackend.domain.album.entity.AlbumEntity;
import com.snapfit.snapfitbackend.domain.album.entity.AlbumPageEntity;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumPageRepository;
import com.snapfit.snapfitbackend.domain.album.repository.AlbumRepository;
import com.snapfit.snapfitbackend.domain.notification.service.PushNotificationService;
import com.snapfit.snapfitbackend.domain.order.dto.OrderQuoteResponse;
import com.snapfit.snapfitbackend.domain.order.dto.OrderResponse;
import com.snapfit.snapfitbackend.domain.order.dto.OrderPageResponse;
import com.snapfit.snapfitbackend.domain.order.dto.OrderSummaryResponse;
import com.snapfit.snapfitbackend.domain.order.dto.PrintPackageResponse;
import com.snapfit.snapfitbackend.domain.order.entity.OrderEntity;
import com.snapfit.snapfitbackend.domain.order.entity.OrderStatus;
import com.snapfit.snapfitbackend.domain.order.print.PrintSubmissionResult;
import com.snapfit.snapfitbackend.domain.order.print.PrintVendorAdapter;
import com.snapfit.snapfitbackend.domain.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PushNotificationService pushNotificationService;
    private final PrintVendorAdapter printVendorAdapter;
    private final AlbumRepository albumRepository;
    private final AlbumPageRepository albumPageRepository;
    private final ObjectMapper objectMapper;

    @Value("${snapfit.order.pricing.base-pages:12}")
    private int basePages;

    @Value("${snapfit.order.pricing.max-pages:50}")
    private int maxPages;

    @Value("${snapfit.order.pricing.base-price:19900}")
    private int basePrice;

    @Value("${snapfit.order.pricing.extra-page-price:700}")
    private int extraPagePrice;

    @Value("${snapfit.order.idempotency-window-seconds:300}")
    private int idempotencyWindowSeconds;

    @Value("${snapfit.order.public-base-url:}")
    private String orderPublicBaseUrl;

    @Transactional(readOnly = true)
    public List<OrderResponse> listByUser(String userId) {
        validateUserId(userId);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listByUserPaged(
            String userId,
            List<String> statusFilters,
            int page,
            int size
    ) {
        validateUserId(userId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<OrderEntity> result;
        List<OrderStatus> statuses = parseStatuses(statusFilters);
        if (statuses.isEmpty()) {
            result = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            result = orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, statuses, pageable);
        }

        return OrderPageResponse.builder()
                .items(result.getContent().stream().map(OrderResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse summarizeByUser(String userId) {
        validateUserId(userId);
        LocalDateTime latestUpdatedAt = orderRepository
                .findFirstByUserIdOrderByUpdatedAtDesc(userId)
                .map(OrderEntity::getUpdatedAt)
                .orElse(null);
        return OrderSummaryResponse.builder()
                .paymentPending(orderRepository.countByUserIdAndStatus(userId, OrderStatus.PAYMENT_PENDING))
                .paymentCompleted(orderRepository.countByUserIdAndStatus(userId, OrderStatus.PAYMENT_COMPLETED))
                .inProduction(orderRepository.countByUserIdAndStatus(userId, OrderStatus.IN_PRODUCTION))
                .shipping(orderRepository.countByUserIdAndStatus(userId, OrderStatus.SHIPPING))
                .delivered(orderRepository.countByUserIdAndStatus(userId, OrderStatus.DELIVERED))
                .canceled(orderRepository.countByUserIdAndStatus(userId, OrderStatus.CANCELED))
                .latestUpdatedAt(latestUpdatedAt)
                .build();
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listAdminPaged(
            List<String> statusFilters,
            String keyword,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);

        List<OrderStatus> statuses = parseStatuses(statusFilters);
        List<OrderStatus> queryStatuses = statuses.isEmpty()
                ? List.of(OrderStatus.PAYMENT_PENDING)
                : statuses;
        String queryKeyword = keyword == null ? "" : keyword.trim();
        Page<OrderEntity> result = orderRepository.searchAdminOrders(
                queryKeyword,
                statuses.isEmpty(),
                queryStatuses,
                pageable);

        return OrderPageResponse.builder()
                .items(result.getContent().stream().map(OrderResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summarizeAdmin() {
        long paymentPending = orderRepository.countByStatus(OrderStatus.PAYMENT_PENDING);
        long paymentCompleted = orderRepository.countByStatus(OrderStatus.PAYMENT_COMPLETED);
        long inProduction = orderRepository.countByStatus(OrderStatus.IN_PRODUCTION);
        long shipping = orderRepository.countByStatus(OrderStatus.SHIPPING);
        long delivered = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long canceled = orderRepository.countByStatus(OrderStatus.CANCELED);
        long total = paymentPending + paymentCompleted + inProduction + shipping + delivered + canceled;
        return Map.of(
                "total", total,
                "paymentPending", paymentPending,
                "paymentCompleted", paymentCompleted,
                "inProduction", inProduction,
                "shipping", shipping,
                "delivered", delivered,
                "canceled", canceled);
    }

    @Transactional
    public OrderResponse createOrReplaceBillingOrder(
            String userId,
            String orderId,
            String title,
            int amount,
            OrderStatus status
    ) {
        validateUserId(userId);

        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseGet(OrderEntity::new);

        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setTitle(title == null || title.isBlank() ? "SnapFit 주문" : title);
        order.setAmount(Math.max(0, amount));
        order.setStatus(status == null ? OrderStatus.PAYMENT_PENDING : status);

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse createPrintOrder(
            String userId,
            Long albumId,
            String title,
            int amount,
            Integer pageCount,
            String paymentMethod,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String addressLine1,
            String addressLine2,
            String deliveryMemo
    ) {
        validateUserId(userId);
        requireText(recipientName, "recipientName");
        requireText(recipientPhone, "recipientPhone");
        requireText(zipCode, "zipCode");
        requireText(addressLine1, "addressLine1");

        OrderQuoteResponse quote = quote(albumId, pageCount);
        String normalizedTitle = title == null || title.isBlank() ? "포토북 주문" : title.trim();
        String normalizedPaymentMethod = trimOrNull(paymentMethod);
        String normalizedRecipientName = trimOrNull(recipientName);
        String normalizedRecipientPhone = normalizeDigits(recipientPhone);
        String normalizedZipCode = normalizeDigits(zipCode);
        String normalizedAddressLine1 = trimOrNull(addressLine1);
        String normalizedAddressLine2 = trimOrNull(addressLine2);
        String normalizedDeliveryMemo = trimOrNull(deliveryMemo);

        Optional<OrderEntity> duplicate = findRecentPendingOrder(userId, albumId);
        if (duplicate.isPresent()) {
            OrderEntity existing = duplicate.get();
            boolean sameOrderIntent =
                    equalsNormalized(existing.getTitle(), normalizedTitle) &&
                    equalsNormalized(existing.getPaymentMethod(), normalizedPaymentMethod) &&
                    equalsNormalized(existing.getRecipientName(), normalizedRecipientName) &&
                    equalsNormalized(normalizeDigits(existing.getRecipientPhone()), normalizedRecipientPhone) &&
                    equalsNormalized(normalizeDigits(existing.getZipCode()), normalizedZipCode) &&
                    equalsNormalized(existing.getAddressLine1(), normalizedAddressLine1) &&
                    equalsNormalized(existing.getAddressLine2(), normalizedAddressLine2) &&
                    equalsNormalized(existing.getDeliveryMemo(), normalizedDeliveryMemo) &&
                    existing.getPageCount() == quote.getPageCount() &&
                    existing.getAmount() == quote.getAmount();
            if (sameOrderIntent) {
                return OrderResponse.from(existing);
            }
        }

        String orderId = buildOrderId();
        OrderEntity order = OrderEntity.builder()
                .orderId(orderId)
                .userId(userId)
                .albumId(albumId)
                .title(normalizedTitle)
                .amount(quote.getAmount())
                .pageCount(quote.getPageCount())
                .status(OrderStatus.PAYMENT_PENDING)
                .paymentMethod(normalizedPaymentMethod)
                .recipientName(normalizedRecipientName)
                .recipientPhone(normalizedRecipientPhone)
                .zipCode(normalizedZipCode)
                .addressLine1(normalizedAddressLine1)
                .addressLine2(normalizedAddressLine2)
                .deliveryMemo(normalizedDeliveryMemo)
                .build();
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirmPaymentAndSubmitPrint(String orderId) {
        OrderEntity order = load(orderId);
        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("주문 상태상 결제 확정을 진행할 수 없습니다: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        order.setPaymentConfirmedAt(LocalDateTime.now());
        PrintPackageResponse printPackage = buildPrintPackage(order);
        order.setPrintPackageJsonUrl(buildPrintPackageUrl(order.getOrderId(), "print-package"));
        order.setPrintFileZipUrl(buildPrintPackageUrl(order.getOrderId(), "print-package.zip"));
        order.setPrintFilePdfUrl(buildPrintPackageUrl(order.getOrderId(), "print-package.pdf"));
        order.setPrintAssetCount(printPackage.getAssets().size());
        order.setPrintPackageGeneratedAt(printPackage.getGeneratedAt());

        PrintSubmissionResult result = printVendorAdapter.submit(order);
        if (!result.accepted()) {
            throw new IllegalStateException("인쇄소 접수 실패: " + result.message());
        }

        order.setPrintVendor(result.vendor());
        order.setPrintVendorOrderId(result.vendorOrderId());
        order.setPrintSubmittedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.IN_PRODUCTION);
        OrderEntity saved = orderRepository.save(order);

        notifyStatusChanged(saved);
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse preparePrintPackage(String orderId) {
        OrderEntity order = load(orderId);
        PrintPackageResponse printPackage = buildPrintPackage(order);
        order.setPrintPackageJsonUrl(buildPrintPackageUrl(order.getOrderId(), "print-package"));
        order.setPrintFileZipUrl(buildPrintPackageUrl(order.getOrderId(), "print-package.zip"));
        order.setPrintFilePdfUrl(buildPrintPackageUrl(order.getOrderId(), "print-package.pdf"));
        order.setPrintAssetCount(printPackage.getAssets().size());
        order.setPrintPackageGeneratedAt(printPackage.getGeneratedAt());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public PrintPackageResponse getPrintPackage(String orderId) {
        return buildPrintPackage(load(orderId));
    }

    @Transactional(readOnly = true)
    public byte[] exportPrintPackageZip(String orderId) {
        PrintPackageResponse printPackage = buildPrintPackage(load(orderId));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                addZipEntry(
                        zip,
                        "print-package.json",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(printPackage)
                );

                List<String> missing = new ArrayList<>();
                for (PrintPackageResponse.PrintAsset asset : printPackage.getAssets()) {
                    String sourceUrl = firstNonBlank(
                            asset.getOriginalUrl(),
                            asset.getPreviewUrl(),
                            asset.getFallbackUrl(),
                            asset.getThumbnailUrl()
                    );
                    if (sourceUrl == null) {
                        missing.add(asset.getId() + " - no image url");
                        continue;
                    }
                    byte[] bytes = downloadBytes(sourceUrl);
                    if (bytes == null || bytes.length == 0) {
                        missing.add(asset.getId() + " - download failed: " + sourceUrl);
                        continue;
                    }
                    addZipEntry(zip, "images/" + asset.getFileName() + extensionOf(sourceUrl), bytes);
                }

                if (!missing.isEmpty()) {
                    addZipEntry(zip, "missing-assets.txt", String.join("\n", missing).getBytes(StandardCharsets.UTF_8));
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("인쇄 ZIP 생성에 실패했습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportPrintPackagePdf(String orderId) {
        PrintPackageResponse printPackage = buildPrintPackage(load(orderId));
        try (PDDocument doc = new PDDocument()) {
            int exportedPages = 0;
            for (PrintPackageResponse.PrintAsset asset : printPackage.getAssets()) {
                if (!"cover".equals(asset.getType()) && !"page".equals(asset.getType())) {
                    continue;
                }
                String sourceUrl = firstNonBlank(
                        asset.getOriginalUrl(),
                        asset.getPreviewUrl(),
                        asset.getFallbackUrl(),
                        asset.getThumbnailUrl()
                );
                byte[] bytes = sourceUrl == null ? null : downloadBytes(sourceUrl);
                if (bytes == null || bytes.length == 0) {
                    continue;
                }

                PDImageXObject image = PDImageXObject.createFromByteArray(doc, bytes, asset.getFileName());
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                drawImageContained(doc, page, image);
                exportedPages++;
            }

            if (exportedPages == 0) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                    stream.beginText();
                    stream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                    stream.newLineAtOffset(72, page.getMediaBox().getHeight() - 96);
                    stream.showText("SnapFit print package");
                    stream.newLineAtOffset(0, -26);
                    stream.setFont(PDType1Font.HELVETICA, 11);
                    stream.showText("No downloadable page images were found. Use print-package.json instead.");
                    stream.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("인쇄 PDF 생성에 실패했습니다.", e);
        }
    }

    @Transactional
    public OrderResponse markShipped(String orderId, String courier, String trackingNumber) {
        OrderEntity order = load(orderId);
        if (order.getStatus() != OrderStatus.IN_PRODUCTION && order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("배송 처리 가능한 상태가 아닙니다: " + order.getStatus());
        }
        requireText(courier, "courier");
        requireText(trackingNumber, "trackingNumber");

        order.setCourier(trimOrNull(courier));
        order.setTrackingNumber(trimOrNull(trackingNumber));
        order.setShippedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPING);
        OrderEntity saved = orderRepository.save(order);

        notifyStatusChanged(saved);
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse markDelivered(String orderId) {
        OrderEntity order = load(orderId);
        if (order.getStatus() != OrderStatus.SHIPPING && order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("배송 완료 처리 가능한 상태가 아닙니다: " + order.getStatus());
        }
        order.setDeliveredAt(LocalDateTime.now());
        order.setStatus(OrderStatus.DELIVERED);
        OrderEntity saved = orderRepository.save(order);

        notifyStatusChanged(saved);
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse setStatus(String orderId, OrderStatus status) {
        OrderEntity order = load(orderId);
        order.setStatus(status);
        OrderEntity saved = orderRepository.save(order);
        notifyStatusChanged(saved);
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse advanceStatus(String orderId) {
        OrderEntity order = load(orderId);

        OrderStatus current = order.getStatus();
        OrderStatus next = current.next();
        order.setStatus(next);

        OrderEntity saved = orderRepository.save(order);
        if (next != current) {
            notifyStatusChanged(saved);
        }

        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse createTestOrder(String userId, String title, int amount) {
        validateUserId(userId);
        String orderId = buildOrderId();

        OrderEntity order = OrderEntity.builder()
                .orderId(orderId)
                .userId(userId)
                .title(title == null || title.isBlank() ? "테스트 포토북 주문" : title)
                .amount(amount <= 0 ? 34900 : amount)
                .pageCount(basePages)
                .status(OrderStatus.PAYMENT_PENDING)
                .recipientName("테스트 수령인")
                .recipientPhone("010-0000-0000")
                .zipCode("00000")
                .addressLine1("서울시 테스트구 테스트로 1")
                .addressLine2("101호")
                .build();

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderQuoteResponse quote(Long albumId, Integer requestPageCount) {
        int pageCount = resolveBillablePageCount(albumId, requestPageCount);
        int extraPageCount = Math.max(0, pageCount - basePages);
        int amount = Math.max(0, basePrice + (extraPageCount * extraPagePrice));
        return OrderQuoteResponse.builder()
                .albumId(albumId)
                .pageCount(pageCount)
                .basePages(basePages)
                .basePrice(basePrice)
                .extraPageCount(extraPageCount)
                .extraPagePrice(extraPagePrice)
                .amount(amount)
                .build();
    }

    private String buildOrderId() {
        return "SF-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private PrintPackageResponse buildPrintPackage(OrderEntity order) {
        AlbumEntity album = order.getAlbumId() == null
                ? null
                : albumRepository.findById(order.getAlbumId()).orElse(null);

        List<PrintPackageResponse.PrintAsset> assets = new ArrayList<>();
        if (album != null) {
            String coverOriginalUrl = trimOrNull(album.getCoverOriginalUrl());
            String coverPreviewUrl = firstNonBlank(album.getCoverPreviewUrl(), album.getCoverImageUrl());
            String coverFallbackUrl = firstNonBlank(coverOriginalUrl, coverPreviewUrl, album.getCoverThumbnailUrl());
            assets.add(PrintPackageResponse.PrintAsset.builder()
                    .id("cover")
                    .type("cover")
                    .pageNumber(0)
                    .fileName("cover")
                    .originalUrl(coverOriginalUrl)
                    .previewUrl(coverPreviewUrl)
                    .thumbnailUrl(trimOrNull(album.getCoverThumbnailUrl()))
                    .fallbackUrl(coverFallbackUrl)
                    .layersJson(album.getCoverLayersJson())
                    .build());

            albumPageRepository.findByAlbumId(album.getId()).stream()
                    .sorted(Comparator.comparingInt(AlbumPageEntity::getPageNumber))
                    .forEach(page -> {
                        String originalUrl = trimOrNull(page.getOriginalUrl());
                        String previewUrl = firstNonBlank(page.getPreviewUrl(), page.getImageUrl());
                        String fallbackUrl = firstNonBlank(originalUrl, previewUrl, page.getThumbnailUrl());
                        assets.add(PrintPackageResponse.PrintAsset.builder()
                                .id("page-" + page.getPageNumber())
                                .type("page")
                                .pageNumber(page.getPageNumber())
                                .fileName("page_%03d".formatted(page.getPageNumber()))
                                .originalUrl(originalUrl)
                                .previewUrl(previewUrl)
                                .thumbnailUrl(trimOrNull(page.getThumbnailUrl()))
                                .fallbackUrl(fallbackUrl)
                                .layersJson(page.getLayersJson())
                                .build());
                    });
        }

        return PrintPackageResponse.builder()
                .orderId(order.getOrderId())
                .albumId(order.getAlbumId())
                .albumTitle(album == null ? order.getTitle() : album.getTitle())
                .ratio(album == null ? null : album.getRatio())
                .pageCount(order.getPageCount())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .zipCode(order.getZipCode())
                .addressLine1(order.getAddressLine1())
                .addressLine2(order.getAddressLine2())
                .deliveryMemo(order.getDeliveryMemo())
                .generatedAt(LocalDateTime.now())
                .assets(assets)
                .build();
    }

    private String buildPrintPackageUrl(String orderId, String assetName) {
        String path = "/api/orders/admin/" + orderId + "/" + assetName;
        String base = trimOrNull(orderPublicBaseUrl);
        if (base == null) return path;
        return base.endsWith("/") ? base.substring(0, base.length() - 1) + path : base + path;
    }

    private void drawImageContained(PDDocument doc, PDPage page, PDImageXObject image) throws IOException {
        PDRectangle box = page.getMediaBox();
        float margin = 0;
        float availableWidth = box.getWidth() - (margin * 2);
        float availableHeight = box.getHeight() - (margin * 2);
        float scale = Math.min(availableWidth / image.getWidth(), availableHeight / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        float x = (box.getWidth() - width) / 2;
        float y = (box.getHeight() - height) / 2;
        try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
            stream.drawImage(image, x, y, width, height);
        }
    }

    private void addZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private byte[] downloadBytes(String sourceUrl) {
        try {
            URLConnection connection = new URL(sourceUrl).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "SnapFit-PrintExporter/1.0");
            try (var in = connection.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extensionOf(String sourceUrl) {
        String lower = sourceUrl.toLowerCase(Locale.ROOT);
        int queryIdx = lower.indexOf('?');
        if (queryIdx >= 0) {
            lower = lower.substring(0, queryIdx);
        }
        if (lower.endsWith(".png")) return ".png";
        if (lower.endsWith(".webp")) return ".webp";
        if (lower.endsWith(".jpeg")) return ".jpeg";
        return ".jpg";
    }

    private Optional<OrderEntity> findRecentPendingOrder(String userId, Long albumId) {
        Optional<OrderEntity> candidate = albumId != null
                ? orderRepository.findFirstByUserIdAndAlbumIdAndStatusOrderByCreatedAtDesc(
                userId,
                albumId,
                OrderStatus.PAYMENT_PENDING)
                : orderRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, OrderStatus.PAYMENT_PENDING);

        if (candidate.isEmpty()) return Optional.empty();
        LocalDateTime createdAt = candidate.get().getCreatedAt();
        if (createdAt == null) return Optional.empty();
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(Math.max(30, idempotencyWindowSeconds));
        if (createdAt.isBefore(threshold)) return Optional.empty();
        return candidate;
    }

    private String normalizeDigits(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private boolean equalsNormalized(String a, String b) {
        String aa = trimOrNull(a);
        String bb = trimOrNull(b);
        if (aa == null && bb == null) return true;
        if (aa == null || bb == null) return false;
        return aa.equals(bb);
    }

    private OrderEntity load(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private void notifyStatusChanged(OrderEntity order) {
        String deeplink = "snapfit://order/detail?orderId=" + order.getOrderId();
        pushNotificationService.notifyOrderStatus(
                order.getOrderId(),
                order.getStatus().name(),
                "주문 상태가 " + order.getStatus().getLabel() + "으로 변경되었습니다.",
                deeplink,
                order.getCourier(),
                order.getTrackingNumber());
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String trimmed = trimOrNull(value);
            if (trimmed != null) return trimmed;
        }
        return null;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static List<OrderStatus> parseStatuses(List<String> filters) {
        if (filters == null || filters.isEmpty()) return List.of();
        return filters.stream()
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return OrderStatus.valueOf(s);
                    } catch (Exception ignored) {
                        return null;
                    }
                })
                .filter(s -> s != null)
                .toList();
    }

    private int resolveBillablePageCount(Long albumId, Integer requestPageCount) {
        int fallback = requestPageCount == null ? basePages : requestPageCount;
        fallback = Math.max(basePages, fallback);

        if (albumId == null) {
            return Math.min(maxPages, fallback);
        }

        long count = albumPageRepository.countByAlbumId(albumId);
        if (count <= 0) {
            AlbumEntity album = albumRepository.findById(albumId).orElse(null);
            if (album != null) {
                Integer totalPages = album.getTotalPages();
                Integer targetPages = album.getTargetPages();
                if (totalPages != null && totalPages > 0) {
                    count = totalPages;
                } else if (targetPages != null && targetPages > 0) {
                    count = targetPages;
                }
            }
        }

        if (count <= 0) {
            count = fallback;
        }
        return (int) Math.max(basePages, Math.min(maxPages, count));
    }
}
