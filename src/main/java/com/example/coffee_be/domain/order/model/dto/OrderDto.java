package com.example.coffee_be.domain.order.model.dto;

import com.example.coffee_be.domain.cartItem.model.CartItemDto;
import com.example.coffee_be.domain.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public record OrderDto(
        Long orderId,
        Long customerId,
        Long cartId,
        List<CartItemDto> items,
        int totalPrice,
        OrderStatus orderStatus) {
}


