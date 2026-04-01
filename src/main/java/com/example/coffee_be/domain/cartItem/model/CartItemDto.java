package com.example.coffee_be.domain.cartItem.model;

import com.example.coffee_be.common.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long menuId;
    private String menuName;
    private int price;
    private int quantity;
    private int totalPrice;


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
