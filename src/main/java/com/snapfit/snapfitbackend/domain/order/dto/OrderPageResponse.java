package com.snapfit.snapfitbackend.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderPageResponse {
    private List<OrderResponse> items;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
}
