package com.example.coffee_be.bulkTest;

// 조회 방식별 성능 비교
// ai 검색 기능은 비용이 발생할 거 같아서 제외, es 는 다른 테스트에 있음
// v1 v2 v3 성능비교

import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import com.example.coffee_be.domain.menu.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // 최초 1번만 데이터가 들어가게 하려면 이거 붙이면 된다고 함
class BulkTest {

    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private MenuService menuService;

    private static final int BULK_SIZE = 10000;

    @BeforeEach
    void setUp() {
        // 1000개씩 배치 insert
        List<Menu> menus = new ArrayList<>();

        // 아메리카노0, 라떼1, 프라푸치노2, 티3, 에이드4, 초코5, 아메6 ...
        // 3000, 3500, 4000, .... 7500, 3000, ...
        String[] categories = {"아메리카노","라떼","프라푸치노","티","에이드","초코"};
        for (int i = 0; i < BULK_SIZE; i++) {
            menus.add(Menu.createMenu(categories[i % 6] + i, 3000 + (i%10)*500, "설명"));
        }

        // BULK_SIZE 만개이므로 천개씩 잘라서 저장(0 ~ 999, 1000~ 1999)
        int batchSize = 1000;
        // todo:
        for (int i = 0; i < menus.size(); i += batchSize) {
            List<Menu> batch = menus.subList(i, Math.min(i+batchSize, menus.size()));
            List<Menu> saved = menuRepository.saveAllAndFlush(batch);
        }
    }

    /// ///////////////////// 하나씩 ///////////////////////////////////////////////
    @Test
    @DisplayName("V1 - JPQL Projection 조회 (10000건)")
    void testV1Projection() {
        long start = System.currentTimeMillis();

        var result = menuService.getAllV1Projection(PageRequest.of(0, 100));

        long elapsed = System.currentTimeMillis() - start;
        log.info("[V1] JPQL Projection - 총 {}건, 소요시간: {}ms", result.getTotalElements(), elapsed);

        assertThat(result.getContent()).isNotEmpty();
        assertThat(elapsed).isLessThan(3000);
    }

    @Test
    @DisplayName("V2 - QueryDSL 조회 (10000건)")
    void testV2QueryDsl() {
        long start = System.currentTimeMillis();

        var result = menuService.getAllV2DSL(PageRequest.of(0, 100));

        long elapsed = System.currentTimeMillis() - start;
        log.info("[V2] QueryDSL - 총 {}건, 소요시간: {}ms", result.getTotalElements(), elapsed);

        assertThat(result.getContent()).isNotEmpty();
        assertThat(elapsed).isLessThan(3000);
    }

    @Test
    @DisplayName("V3 - Cursor 기반 페이지네이션 (10000건)")
    void testV3Cursor() {
        long start = System.currentTimeMillis();

        var result = menuService.getAllV3Cursor(null, 100);

        long elapsed = System.currentTimeMillis() - start;
        log.info("[V3] Cursor - 조회건수: {}건, 다음페이지: {}, 소요시간: {}ms",
                result.getMenus().size(), result.isHasNext(), elapsed);

        assertThat(result.getMenus()).isNotEmpty();
        assertThat(result.isHasNext()).isTrue();
        assertThat(elapsed).isLessThan(3000);
    }

    @Test
    @DisplayName("V3 - Cursor 연속 페이지 조회 (중복 없음 확인)")
    void testV3CursorPaging() {
        var page1 = menuService.getAllV3Cursor(null, 100);
        assertThat(page1.getMenus()).hasSize(100);
        assertThat(page1.isHasNext()).isTrue();

        var page2 = menuService.getAllV3Cursor(page1.getNextCursorId(), 100);
        assertThat(page2.getMenus()).isNotEmpty();

        // 1페이지와 2페이지 id 중복 없음 확인
        var page1Ids = page1.getMenus().stream().map(MenuDto::id).toList();
        var page2Ids = page2.getMenus().stream().map(MenuDto::id).toList();
        assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);

        log.info("[V3] 페이지1: {}건, 페이지2: {}건 - 중복없음 확인", page1Ids.size(), page2Ids.size());
    }

    /// ///////////////////// 종합 ///////////////////////////////////////////////
    @Test
    @DisplayName("조회 방식별 성능 종합 비교")
    void testPerformanceComparison() {
        long s1 = System.currentTimeMillis();
        menuService.getAllV1Projection(PageRequest.of(0, 100));
        long v1 = System.currentTimeMillis() - s1;

        long s2 = System.currentTimeMillis();
        menuService.getAllV2DSL(PageRequest.of(0, 100));
        long v2 = System.currentTimeMillis() - s2;

        long s3 = System.currentTimeMillis();
        menuService.getAllV3Cursor(null, 100);
        long v3 = System.currentTimeMillis() - s3;

        log.info("========== 조회 성능 비교 (10000건) ==========");
        log.info("V1 (JPQL Projection) : {}ms", v1);
        log.info("V2 (QueryDSL)        : {}ms", v2);
        log.info("V3 (Cursor)          : {}ms", v3);
        log.info("=============================================");

        assertThat(v1).isLessThan(3000);
        assertThat(v2).isLessThan(3000);
        assertThat(v3).isLessThan(3000);
    }
}
