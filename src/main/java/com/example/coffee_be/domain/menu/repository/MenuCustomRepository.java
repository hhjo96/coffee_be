package com.example.coffee_be.domain.menu.repository;

import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import com.example.coffee_be.domain.menu.model.response.MenuCursorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MenuCustomRepository {

    Page<MenuDto> getAllV2(Pageable pageable);
    MenuCursorResponse getAllV3(Long cursorId, int size);
}
