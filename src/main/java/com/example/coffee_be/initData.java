package com.example.coffee_be;

import com.example.coffee_be.common.entity.Customer;
import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.common.entity.Point;
import com.example.coffee_be.domain.customer.repository.CustomerRepository;
import com.example.coffee_be.common.entity.Point;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import com.example.coffee_be.domain.point.repository.PointRepository;
import com.example.coffee_be.domain.search.document.MenuDocument;
import com.example.coffee_be.domain.search.service.MenuSearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(
        name = "app.init-data",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
public class initData {

    private final MenuRepository menuRepository;
    private final CustomerRepository customerRepository;
    private final PointRepository pointRepository;
    private final MenuSearchService menuSearchService;

    @PostConstruct
    @Transactional
    public void init() {
        // 메뉴
        List<Menu> menus = menuRepository.saveAllAndFlush(List.of(
                // 에스프레소
                Menu.createMenu("에스프레소",          4000, "진한 에스프레소 샷"),
                Menu.createMenu("아메리카노",           4500, "에스프레소에 물을 더한 음료"),
                Menu.createMenu("아이스아메리카노",     4500, "차갑게 즐기는 아메리카노"),
                Menu.createMenu("콜드브루",             5000, "차갑게 장시간 추출한 커피"),
                // 라떼
                Menu.createMenu("카페라떼",             5000, "에스프레소와 스팀밀크"),
                Menu.createMenu("아이스카페라떼",       5000, "차갑게 즐기는 카페라떼"),
                Menu.createMenu("바닐라라떼",           5500, "바닐라 시럽이 들어간 라떼"),
                Menu.createMenu("아이스바닐라라떼",     5500, "차갑게 즐기는 바닐라라떼"),
                Menu.createMenu("카라멜라떼",           5500, "카라멜 시럽이 들어간 라떼"),
                // 프라푸치노
                Menu.createMenu("카라멜프라푸치노",     6500, "카라멜과 커피의 블렌디드 음료"),
                Menu.createMenu("자바칩프라푸치노",     6500, "초콜릿칩이 들어간 블렌디드 음료"),
                Menu.createMenu("녹차프라푸치노",       6000, "말차가 들어간 블렌디드 음료"),
                Menu.createMenu("딸기프라푸치노",       6500, "딸기가 들어간 블렌디드 음료"),
                Menu.createMenu("망고프라푸치노",       6500, "망고가 들어간 블렌디드 음료"),
                // 티
                Menu.createMenu("얼그레이티",           4500, "베르가못 향의 홍차"),
                Menu.createMenu("잉글리시브렉퍼스트",   4500, "진한 풍미의 홍차"),
                Menu.createMenu("민트티",               4500, "상쾌한 민트 허브티"),
                Menu.createMenu("캐모마일티",           4500, "은은한 캐모마일 허브티"),
                Menu.createMenu("녹차라떼",             5500, "말차와 스팀밀크의 조화"),
                Menu.createMenu("아이스녹차라떼",       5500, "차갑게 즐기는 녹차라떼"),
                Menu.createMenu("밀크티",               5000, "홍차와 우유의 조화"),
                Menu.createMenu("아이스밀크티",         5000, "차갑게 즐기는 밀크티"),
                // 기타음료
                Menu.createMenu("핫초코",               5000, "진한 초콜릿 음료"),
                Menu.createMenu("아이스초코",           5000, "차갑게 즐기는 초코음료"),
                Menu.createMenu("레모네이드",           5000, "상큼한 레몬 에이드"),
                Menu.createMenu("자몽에이드",           5500, "자몽의 상큼한 에이드"),
                Menu.createMenu("복숭아아이스티",       5000, "복숭아향 아이스티"),
                Menu.createMenu("스파클링레몬",         4500, "탄산이 들어간 레몬음료"),
                Menu.createMenu("딸기레모네이드",       5500, "딸기와 레몬의 상큼한 조화")
        ));

        menuSearchService.saveAll(
                menus.stream().map(MenuDocument::from).toList()
        );

        // menu.getName 만 넣을경우 메뉴 이름만 가지고 임베딩함
        // 가격까지 넣더라도 "저렴한거" 를 찾지는 못했음
        // 숫자 계산이 아니라 의미 유사도 기반이라서
        List<MenuDocument> menuDocuments = menus.stream()
                .map(menu -> MenuDocument.from(
                        menu, menuSearchService.embed(menu.getName() + " " + menu.getPrice() + "원 " + menu.getDescription())))
                .toList();
        menuSearchService.saveAll(menuDocuments);


        // 고객
        Customer customer1 = Customer.createCustomer("홍길동");
        Customer customer2 = Customer.createCustomer("김영희");

        customerRepository.saveAndFlush(customer1);
        customerRepository.saveAndFlush(customer2);


        // 초기 포인트 지급
        Point point1 = Point.createPoint(customer1.getId());
        point1.charge(20000);
        pointRepository.saveAndFlush(point1);
        Point point2 = Point.createPoint(customer2.getId());
        point2.charge(10000);
        pointRepository.saveAndFlush(point2);

    }
}
