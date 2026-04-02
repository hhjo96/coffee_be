package com.example.coffee_be.domain.order.service;

import com.example.coffee_be.common.entity.Cart;
import com.example.coffee_be.common.entity.CartItem;
import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.common.entity.Order;
import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.common.model.kafka.topic.KafkaTopics;
import com.example.coffee_be.domain.cart.Service.CartService;
import com.example.coffee_be.domain.cart.repository.CartRepository;
import com.example.coffee_be.domain.cartItem.model.CartItemDto;
import com.example.coffee_be.domain.cartItem.repository.CartItemRepository;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import com.example.coffee_be.domain.order.enums.OrderStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PointService pointService;
    private final CartService cartService;
    private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int POPULAR_MENU_LIMIT = 3;
    private static final String LOCK_ORDER_PREFIX = "lock:order:";
    private static final String RANKING_KEY = "ranking:menus";


    // 주문+결제
    // 포인트 차감은 pointService.userPointForOrder()
    public OrderDto order(OrderRequest request) {
        Long userId = request.getCustomerId();
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
        Long userId = request.getCustomerId();
        Long cartId = request.getCartId();

        // 1. 카트 조회
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_CART));
        if (!cart.getCustomerId().equals(userId))
            throw new ServiceErrorException(ErrorEnum.NOT_CART_OWNER);

        // 2. 카트아이템 조회
        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cartId);
        if (cartItems.isEmpty())
            throw new ServiceErrorException(ErrorEnum.CART_EMPTY);

        // 3. 총금액 계산
        int totalPrice = cartItems.stream()
                .mapToInt(CartItem::totalPrice).sum();

        // 4. 메뉴 조회 (삭제된 메뉴 제외)
        for(CartItem item: cartItems) {
            Long menuId = item.getMenuId();
            Menu menu = menuRepository.findById(cartId)
                    .filter(m -> m.getDeletedAt() == null)
                    .orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));
        }

        // 5. 주문 저장
        Order order = Order.createOrder(userId, cartId, totalPrice);
        Order savedOrder = orderRepository.save(order);

        // 6. 포인트 차감 (PointService)
        pointService.usePointForOrder(userId, totalPrice, savedOrder.getId());

        // 7. Kafka 이벤트 발행 (수집 플랫폼으로 실시간 전송)
        List<OrderCompletedEvent.OrderItemEvent> eventItems = cartItems.stream()
                .map(item -> OrderCompletedEvent.OrderItemEvent.builder()
                        .menuId(item.getMenuId())
                        .menuName(item.getMenuName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .totalPrice(item.totalPrice())
                        .build()).toList();


        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(savedOrder.getId())
                .cartId(cartId)
                .userId(userId)
                .items(eventItems)
                .totalPrice(totalPrice)
                .paidAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        kafkaTemplate.send(KafkaTopics.TOPIC_ORDER_COMPLETED, String.valueOf(savedOrder.getId()), event);

        log.info("[주문완료 - 상태: 준비중] orderId={}, userId={}, menuId={}, price={}",
                savedOrder.getId(), userId, cartId, totalPrice);

        // 8. 주문 완료 후 카트 삭제
        cartService.deleteCartAfterOrder(cartId);

        List<CartItemDto> itemDtos = cartItems.stream().map(CartItemDto::from).toList();

        return new OrderDto(savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getCartId(), itemDtos, savedOrder.getPrice(), OrderStatus.PREPARING);
    }

    @Transactional(readOnly = true)
    public List<PopularMenuDto> getPopularMenus() {
        Set<Object> cached = redisTemplate.opsForZSet().reverseRange(RANKING_KEY, 0, POPULAR_MENU_LIMIT - 1);

        // 캐시에 3개이상 있으면
        if (cached != null && cached.size() == POPULAR_MENU_LIMIT) {
            log.info("[인기메뉴] Redis 캐시 히트");
            return buildPopularMenuDtoFromCache(cached);
        }
        // 캐시에 3개미만이거나 없으면
        log.info("[인기메뉴] 캐시 미스 → DB 집계");
        return refreshPopularMenuCache();
    }
    private List<PopularMenuDto> buildPopularMenuDtoFromCache(Set<Object> menuIds) {
        List<PopularMenuDto> result = new ArrayList<>();
        for (Object menuIdTemp : menuIds) {
            Long menuId = Long.parseLong(menuIdTemp.toString());
            Double score = redisTemplate.opsForZSet().score(RANKING_KEY, menuIdTemp);

            menuRepository.findById(menuId).filter(m -> m.getDeletedAt() == null)
                    .ifPresent(menu -> result.add(
                    new PopularMenuDto(menu.getId(), menu.getName(), menu.getPrice(),
                            score != null ? score.longValue() : 0L
                    )));
        }
        return result;
    }
    private List<PopularMenuDto> refreshPopularMenuCache() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> rows = orderRepository.findPopularMenuIdsSince(since, POPULAR_MENU_LIMIT);
        List<PopularMenuDto> result = new ArrayList<>();

        redisTemplate.delete(RANKING_KEY);
        for (Object[] row : rows) {
            Long menuId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            menuRepository.findById(menuId)
                    .filter(m -> m.getDeletedAt() == null)
                    .ifPresent(menu -> {
                        redisTemplate.opsForZSet().add(RANKING_KEY, menuId.toString(), count);
                        result.add(new PopularMenuDto(
                                menu.getId(), menu.getName(), menu.getPrice(), count));
                    });
        }
        redisTemplate.expire(RANKING_KEY, 60, TimeUnit.SECONDS);
        log.info("[인기메뉴] ZSet 갱신 완료, {}개", result.size());
        return result;
    }
}
