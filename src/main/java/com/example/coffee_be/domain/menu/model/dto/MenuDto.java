package com.example.coffee_be.domain.menu.model.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

// 역직렬화 문제로 캐싱 관련 dto에는 해당 어노테이션 필요
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("menuDto")
public record MenuDto(Long id, String name, int price) {}