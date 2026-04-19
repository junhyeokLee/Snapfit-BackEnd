package com.snapfit.snapfitbackend.domain.order.print;

import com.snapfit.snapfitbackend.domain.order.entity.OrderEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class ManualPrintVendorAdapter implements PrintVendorAdapter {

    @Override
    public PrintSubmissionResult submit(OrderEntity order) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        String vendorOrderId = "MANUAL-" + order.getOrderId() + "-" + timestamp + "-" + suffix;
        return new PrintSubmissionResult(
                true,
                "MANUAL",
                vendorOrderId,
                "관리자 인쇄 접수 대기열에 등록되었습니다.");
    }
}

