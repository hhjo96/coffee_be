package com.example.coffee_be.domain.cart.model.dto;

import com.example.coffee_be.domain.cartItem.model.CartItemDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CartDto {
    private Long cartId;
    private Long userId;
    private List<CartItemDto> items;
    private int totalPrice;
}
