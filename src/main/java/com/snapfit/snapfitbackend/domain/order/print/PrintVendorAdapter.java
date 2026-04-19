package com.snapfit.snapfitbackend.domain.order.print;

import com.snapfit.snapfitbackend.domain.order.entity.OrderEntity;

public interface PrintVendorAdapter {
    PrintSubmissionResult submit(OrderEntity order);
}

