package com.example.coffee_be.domain.menu.repository;

import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import com.example.coffee_be.domain.menu.model.response.MenuCursorResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.coffee_be.common.entity.QMenu.menu;


@RequiredArgsConstructor
public class MenuCustomRepositoryImpl implements MenuCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MenuDto> getAllV2(Pageable pageable) {
        List<MenuDto> content = queryFactory
                .select(Projections.constructor(
                        MenuDto.class,
                        menu.id,
                        menu.name,
                        menu.price
                ))
                .from(menu)
                .where(menu.deletedAt.isNull())
                .fetch();

        Long total = queryFactory
                .select(menu.count())
                .from(menu)
                .where(menu.deletedAt.isNull())
                .fetchOne();

        if(total == null) {
            total = 0L;
        }
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public MenuCursorResponse getAllV3(Long cursorId, int size) {
        List<MenuDto> menus = queryFactory
                .select(Projections.constructor(MenuDto.class,
                        menu.id,
                        menu.name,
                        menu.price))
                .from(menu)
                .where(
                        menu.deletedAt.isNull(),
                        cursorId != null ? menu.id.gt(cursorId) : null  // 커서 조건
                )
                .orderBy(menu.id.asc())
                .limit(size + 1)  // 다음 페이지 있는지 확인용으로 1개 더 가져옴
                .fetch();

        boolean hasNext = menus.size() > size;
        if (hasNext) menus.remove(menus.size() - 1);  // 1개 제거

        Long nextCursorId = hasNext ? menus.get(menus.size() - 1).id() : null;

        return new MenuCursorResponse(menus, nextCursorId, hasNext);
    }
}
