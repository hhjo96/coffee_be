package com.example.coffee_be.domain.cartItem.model;

import com.example.coffee_be.common.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record CartItemDto(Long id, Long menuId, String menuName, int price, int quantity, int totalPrice) {

    public static CartItemDto from(CartItem item) {
        return new CartItemDto(
                item.getId(),
                item.getMenuId(),
                item.getMenuName(),
                item.getPrice(),
                item.getQuantity(),
                item.totalPrice()
        );
    }
}