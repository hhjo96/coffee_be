package com.example.coffee_be.domain.cart.model.dto;

import com.example.coffee_be.domain.cartItem.model.CartItemDto;


import java.util.List;

public record CartDto(Long cartId, Long userId, List<CartItemDto> items, int totalPrice) {}

