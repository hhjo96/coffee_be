package com.example.coffee_be.domain.order.model.dto;

import com.example.coffee_be.domain.cartItem.model.CartItemDto;
import com.example.coffee_be.domain.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderDto {

    private Long orderId;
    private Long customerId;
    private Long cartId;
    private List<CartItemDto> items;
    private int totalPrice;
    private OrderStatus orderStatus;

}
