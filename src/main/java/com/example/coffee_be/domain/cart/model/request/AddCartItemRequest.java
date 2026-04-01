package com.example.coffee_be.domain.cart.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AddCartItemRequest {
    @NotNull(message = "메뉴 ID는 필수입니다")
    private Long menuId;
    @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다")
    private int quantity;
}
