package com.example.coffee_be.common.model.kafka.event;

public class PointChargedEvent {

    private Long userId;       // 사용자 ID
    private int amount;      // 충전된 금액
    private String paidAt;     // 충전 완료 시각 (예: "2025-11-28T10:15:30")
}
