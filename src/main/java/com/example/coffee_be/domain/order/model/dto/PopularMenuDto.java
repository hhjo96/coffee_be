package com.example.coffee_be.domain.order.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularMenuDto {

    private Long menuId;
    private String name;
    private int price;
    private Long orderCount;
}
