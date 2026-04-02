package com.example.coffee_be.domain.order.scheduler;

import com.example.coffee_be.common.entity.Order;
import com.example.coffee_be.domain.order.enums.OrderStatus;
import com.example.coffee_be.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

    private final OrderRepository orderRepository;

    // 매 30초마다 실행
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void updateOrderStatusToReady() {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);

        List<Order> orders = orderRepository
                .findAllByOrderStatusAndCreatedAtBefore(OrderStatus.PREPARING, oneMinuteAgo);

        if (orders.isEmpty()) return;

        orders.forEach(Order::prepareToReady);

        log.info("[스케줄러] PREPARING → READY 변경 완료, {}건", orders.size());
    }
}
