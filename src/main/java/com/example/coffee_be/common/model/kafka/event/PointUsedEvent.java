package com.example.coffee_be.common.model.kafka.event;

public class PointUsedEvent {

    private Long userId;       // 사용자 ID
    private int quantity;      // 결제된 상품 수량
    private String paidAt;     // 결제 완료 시각 (예: "2025-11-28T10:15:30")
}
