package com.example.coffee_be;

import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "app.init-data",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
public class initData {

    private final MenuRepository menuRepository;

    @PostConstruct
    @Transactional
    public void init() {
        Menu menu = Menu.createMenu("아메리카노", 3000, "아메리카노에 대한 설명");

        menuRepository.saveAndFlush(menu);
    }
}
