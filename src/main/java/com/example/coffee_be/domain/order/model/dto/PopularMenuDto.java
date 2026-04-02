package com.example.coffee_be.domain.order.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record PopularMenuDto(Long menuId, String name, int price, Long orderCount) {

}

