package com.snapfit.snapfitbackend.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PrintPackageResponse {
    private String orderId;
    private Long albumId;
    private String albumTitle;
    private String ratio;
    private Integer pageCount;
    private String recipientName;
    private String recipientPhone;
    private String zipCode;
    private String addressLine1;
    private String addressLine2;
    private String deliveryMemo;
    private LocalDateTime generatedAt;
    private List<PrintAsset> assets;

    @Getter
    @Builder
    public static class PrintAsset {
        private String id;
        private String type;
        private Integer pageNumber;
        private String fileName;
        private String originalUrl;
        private String previewUrl;
        private String thumbnailUrl;
        private String fallbackUrl;
        private String layersJson;
    }
}
