package com.example.coffee_be.domain.menu.model.response;

import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MenuCursorResponse {
    private final List<MenuDto> menus;
    private final Long nextCursorId;  // 다음 요청 때 쓸 커서 (마지막 항목 id)
    private final boolean hasNext;    // 다음 페이지 있는지
}