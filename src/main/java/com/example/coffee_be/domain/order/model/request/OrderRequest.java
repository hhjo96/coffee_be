package com.example.coffee_be.domain.order.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    Long userId;
    Long menuId;

}
