package com.example.coffee_be.es;

import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import com.example.coffee_be.domain.search.document.MenuDocument;
import com.example.coffee_be.domain.search.repository.MenuSearchRepository;
import com.example.coffee_be.domain.search.service.MenuSearchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class ESTest {

    @Autowired
    private MenuSearchService menuSearchService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuSearchRepository menuSearchRepository;

    @BeforeAll
    void setUp() {
        // 메뉴
        List<Menu> menus = menuRepository.saveAllAndFlush(List.of(
                // 에스프레소
                Menu.createMenu("에스프레소", 4000, "진한 에스프레소 샷"),
                Menu.createMenu("아메리카노", 4500, "에스프레소에 물을 더한 음료"),
                Menu.createMenu("아이스아메리카노", 4500, "차갑게 즐기는 아메리카노"),
                Menu.createMenu("콜드브루", 5000, "차갑게 장시간 추출한 커피"),
                // 라떼
                Menu.createMenu("카페라떼", 5000, "에스프레소와 스팀밀크"),
                Menu.createMenu("아이스카페라떼", 5000, "차갑게 즐기는 카페라떼"),
                Menu.createMenu("바닐라라떼", 5500, "바닐라 시럽이 들어간 라떼"),
                Menu.createMenu("아이스바닐라라떼", 5500, "차갑게 즐기는 바닐라라떼"),
                Menu.createMenu("카라멜라떼", 5500, "카라멜 시럽이 들어간 라떼"),
                // 프라푸치노
                Menu.createMenu("카라멜프라푸치노", 6500, "카라멜과 커피의 블렌디드 음료"),
                Menu.createMenu("자바칩프라푸치노", 6500, "초콜릿칩이 들어간 블렌디드 음료"),
                Menu.createMenu("녹차프라푸치노", 6000, "말차가 들어간 블렌디드 음료"),
                Menu.createMenu("딸기프라푸치노", 6500, "딸기가 들어간 블렌디드 음료"),
                Menu.createMenu("망고프라푸치노", 6500, "망고가 들어간 블렌디드 음료"),
                // 티
                Menu.createMenu("얼그레이티", 4500, "베르가못 향의 홍차"),
                Menu.createMenu("잉글리시브렉퍼스트", 4500, "진한 풍미의 홍차"),
                Menu.createMenu("민트티", 4500, "상쾌한 민트 허브티"),
                Menu.createMenu("캐모마일티", 4500, "은은한 캐모마일 허브티"),
                Menu.createMenu("녹차라떼", 5500, "말차와 스팀밀크의 조화"),
                Menu.createMenu("아이스녹차라떼", 5500, "차갑게 즐기는 녹차라떼"),
                Menu.createMenu("밀크티", 5000, "홍차와 우유의 조화"),
                Menu.createMenu("아이스밀크티", 5000, "차갑게 즐기는 밀크티"),
                // 기타음료
                Menu.createMenu("핫초코", 5000, "진한 초콜릿 음료"),
                Menu.createMenu("아이스초코", 5000, "차갑게 즐기는 초코음료"),
                Menu.createMenu("레모네이드", 5000, "상큼한 레몬 에이드"),
                Menu.createMenu("자몽에이드", 5500, "자몽의 상큼한 에이드"),
                Menu.createMenu("복숭아아이스티", 5000, "복숭아향 아이스티"),
                Menu.createMenu("스파클링레몬", 4500, "탄산이 들어간 레몬음료"),
                Menu.createMenu("딸기레모네이드", 5500, "딸기와 레몬의 상큼한 조화")
        ));

        menuSearchService.saveAll(
                menus.stream().map(MenuDocument::from).toList()
        );
    }

    @Test
    @DisplayName("nori - 형태소 분석 (아메리카 -> 아메리카노, 아이스아메리카노)")
    void testNoriSearch() {
        long start = System.currentTimeMillis();

        var result = menuSearchService.searchByES("아메리카");

        long elapsed = System.currentTimeMillis() - start;
        log.info("[ES] nori 검색 - 결과: {}건, 소요시간: {}ms", result.size(), elapsed);
        result.forEach(m -> log.info("  → {}", m.name()));

        assertThat(result).isNotEmpty();
        assertThat(elapsed).isLessThan(3000);
    }

    @Test
    @DisplayName("fuzzy - 오타 허용 (아메리가노 -> 아메리카노)")
    void testFuzzySearchGano() {
        var result = menuSearchService.searchByES("아메리가노");

        log.info("[ES] fuzzy 검색 '아메리카' - 결과: {}건", result.size());
        result.forEach(m -> log.info("  → {}", m.name()));

        // 오타가 있어도 아메리카노 계열이 나와야 함
        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(m -> m.name().contains("아메리카노"))).isTrue();
    }


    @Test
    @DisplayName("동의어 - 아아 -> 아이스아메리카노")
    void testSynonymAa() {
        var result = menuSearchService.searchByES("아아");

        log.info("[ES] 동의어 검색 '아아' - 결과: {}건", result.size());
        result.forEach(m -> log.info("  → {}", m.name()));

        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(m -> m.name().contains("아이스아메리카노"))).isTrue();
    }

    @Test
    @DisplayName("동의어 - 뜨아 → 아메리카노")
    void testSynonymHotAme() {
        var result = menuSearchService.searchByES("뜨아");

        log.info("[ES] 동의어 검색 '뜨아' - 결과: {}건", result.size());
        result.forEach(m -> log.info("  → {}", m.name()));

        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(m -> m.name().contains("아메리카노"))).isTrue();
    }

    @Test
    @DisplayName("검색 성능 - 다양한 키워드 응답시간 확인")
    void testSearchPerformance() {
        String[] keywords = {"아메리카", "라떼", "초코", "아아", "바라", "밀티"};

        log.info("========== ES 검색 성능 ==========");
        for (String keyword : keywords) {
            long start = System.currentTimeMillis();
            var result = menuSearchService.searchByES(keyword);
            long elapsed = System.currentTimeMillis() - start;
            log.info("'{}' → {}건, {}ms", keyword, result.size(), elapsed);
            assertThat(elapsed).isLessThan(1000);
        }
        log.info("==================================");
    }
}