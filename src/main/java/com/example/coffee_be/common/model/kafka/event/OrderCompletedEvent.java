package com.example.coffee_be.common.model.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {

   // private Long paymentId;    // 결제 ID (PG사나 내부 결제 번호)
    private Long orderId;      // 주문 번호
    private Long cartId;    // 상품 ID
    private Long userId;       // 사용자 ID
    private List<OrderItemEvent> items;
    private int totalPrice;
    private String paidAt;     // 결제 완료 시각 (예: "2025-11-28T10:15:30")


    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private Long menuId;
        private String menuName;
        private int quantity;    // ✓ 아이템별 수량 정상 포함
        private int price;       // 단가
        private int totalPrice;  // 단가 × 수량
    }
}
