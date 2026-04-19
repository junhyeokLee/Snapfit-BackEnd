package com.snapfit.snapfitbackend.domain.order.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class OrderSummaryResponse {
    private long paymentPending;
    private long paymentCompleted;
    private long inProduction;
    private long shipping;
    private long delivered;
    private long canceled;
    private LocalDateTime latestUpdatedAt;
}
