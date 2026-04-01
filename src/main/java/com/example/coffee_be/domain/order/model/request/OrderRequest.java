package com.example.coffee_be.domain.order.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    @NotNull(message = "사용자번호는 필수입니다")
    Long customerId;

    // 카트 안의 카트아이템을 모두 주문
    @NotNull(message = "카트는 필수입니다")
    Long cartId;

}
