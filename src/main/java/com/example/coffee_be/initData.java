package com.example.coffee_be;

import com.example.coffee_be.common.entity.Customer;
import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.common.entity.Point;
import com.example.coffee_be.domain.customer.repository.CustomerRepository;
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
    private final CustomerRepository customerRepository;

    @PostConstruct
    @Transactional
    public void init() {
        // 메뉴
        Menu menu1 = Menu.createMenu("아메리카노", 3000, "아메리카노에 대한 설명");
        Menu menu2 = Menu.createMenu("라떼", 3500, "라떼에 대한 설명");
        Menu menu3 = Menu.createMenu("바닐라 빈 라떼", 4500, "바닐라 빈 라떼에 대한 설명");
        Menu menu4 = Menu.createMenu("밀크티", 4000, "밀크티에 대한 설명");

        menuRepository.saveAndFlush(menu1);
        menuRepository.saveAndFlush(menu2);
        menuRepository.saveAndFlush(menu3);
        menuRepository.saveAndFlush(menu4);

        // 고객
        Customer customer1 = Customer.createCustomer("홍길동");
        Customer customer2 = Customer.createCustomer("김영희");

        customerRepository.saveAndFlush(customer1);
        customerRepository.saveAndFlush(customer1);


//        // 초기 포인트 지급
//        Point point1 = Point.createPoint(customer1.getId(), 10000);
//        pointRepository.saveAndFlush(point1);
//        Point point2 = Point.createPoint(customer2.getId());
//        point2.charge(20000);
//        pointRepository.saveAndFlush(point2);

    }
}
