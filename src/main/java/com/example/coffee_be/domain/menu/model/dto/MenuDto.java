package com.example.coffee_be.domain.menu.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class MenuDto {

    private Long id;
    private String name;
    private int price;

}
