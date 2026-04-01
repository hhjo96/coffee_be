package com.example.coffee_be.domain.order.service;

import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.common.entity.Order;
import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.common.model.kafka.topic.KafkaTopics;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import com.example.coffee_be.domain.order.model.dto.OrderDto;
import com.example.coffee_be.domain.order.model.dto.PopularMenuDto;
import com.example.coffee_be.domain.order.model.request.OrderRequest;
import com.example.coffee_be.domain.order.repository.OrderRepository;
import com.example.coffee_be.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final PointService pointService;
    private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LOCK_ORDER_PREFIX = "lock:order:";
    private static final String RANKING_KEY = "ranking:menus";


    // 주문+결제
    // 포인트 차감은 pointService.userPointForOrder()
    public OrderDto order(OrderRequest request) {
        Long userId = request.getUserId();
        // 락 키를 userId 기준으로 설정 — 동일 유저의 동시 주문 요청 직렬화
        String lockKey = LOCK_ORDER_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 대기시간 / 락잡고있는최대시간 / 시간단위
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new ServiceErrorException(ErrorEnum.ORDER_LOCK_FAILED);
            }
            return doOrder(request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceErrorException(ErrorEnum.ORDER_LOCK_FAILED);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private OrderDto doOrder(OrderRequest request) {
        Long userId = request.getUserId();
        Long menuId = request.getMenuId();

        // 1. 메뉴 조회 (삭제된 메뉴 제외)
        Menu menu = menuRepository.findById(menuId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));

        // 2. 포인트 차감 (PointService)
        pointService.usePointForOrder(userId, menu.getPrice());

        // 3. 주문 저장
        Order order = Order.createOrder(userId, menuId, menu.getPrice());
        Order savedOrder = orderRepository.save(order);

        // 4. Kafka 이벤트 발행 (수집 플랫폼으로 실시간 전송)
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(savedOrder.getId())
                .productId(menuId)
                .userId(userId)
                .quantity()
                .price(menu.getPrice())
                .paidAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        kafkaTemplate.send(KafkaTopics.TOPIC_ORDER_COMPLETED,
                String.valueOf(savedOrder.getId()), event);
        log.info("[주문완료] orderId={}, userId={}, menuId={}, price={}",
                savedOrder.getId(), userId, menuId, menu.getPrice());
        return OrderDto.from(savedOrder);
    }

    public List<PopularMenuDto> getPopularMenus() {
    }
}
