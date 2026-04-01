package com.example.coffee_be.domain.order.model.dto;

import com.example.coffee_be.domain.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderDto {

    private Long orderId;
    private Long customerId;
    private Long menuId;
    private int price;
    private OrderStatus orderStatus;

}
