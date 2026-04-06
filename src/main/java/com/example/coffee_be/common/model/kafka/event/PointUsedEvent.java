package com.example.coffee_be.common.model.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointUsedEvent {

    private Long userId;       // 사용자 ID
    private Long orderId;
    private int amount;      // 포인트 사용량
    private String paidAt;     // 결제 완료 시각 (예: "2025-11-28T10:15:30")
}
