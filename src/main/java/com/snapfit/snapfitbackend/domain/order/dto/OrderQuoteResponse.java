package com.snapfit.snapfitbackend.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderQuoteResponse {
    private Long albumId;
    private int pageCount;
    private int basePages;
    private int basePrice;
    private int extraPageCount;
    private int extraPagePrice;
    private int amount;
}
