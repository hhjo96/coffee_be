package com.example.coffee_be.domain.order.service;

// 레디스 잔액검증, db 차감

import com.example.coffee_be.common.entity.*;
import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.common.model.kafka.topic.KafkaTopics;
import com.example.coffee_be.domain.History.repository.HistoryRepository;
import com.example.coffee_be.domain.cart.Service.CartService;
import com.example.coffee_be.domain.cart.repository.CartRepository;
import com.example.coffee_be.domain.cartItem.model.CartItemDto;
import com.example.coffee_be.domain.cartItem.repository.CartItemRepository;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import com.example.coffee_be.domain.order.enums.OrderStatus;
import com.example.coffee_be.domain.order.model.dto.OrderDto;
import com.example.coffee_be.domain.order.model.request.OrderRequest;
import com.example.coffee_be.domain.order.repository.OrderRepository;
import com.example.coffee_be.domain.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.coffee_be.common.exception.ErrorEnum.NOT_FOUND_POINT;
import static com.example.coffee_be.common.exception.ErrorEnum.POINT_INSUFFICIENT;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderLuaService {

    // 주문이 들어오면 루아스크립트를 활용해 현재 사용자 포인트를 차감하고 주문 객체 생성함
    // 잔액이 레디스에 있는 경우 사용, 없을 경우 db 조회

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuRepository menuRepository;
    private final CartService cartService;
    private final PointRepository pointRepository;
    private final HistoryRepository historyRepository;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;

    private static final String LOCK_ORDER_PREFIX = "lock:order:";
    private static final String BALANCE_KEY_PREFIX = "point:balance:";

    // Lua 스크립트
    // 잔액을 읽어서 값이 없으면 -1(캐시에없다), 값이 차감할 금액보다 적으면 -2, 아니면 차감
    // 주문 결제 시 → Lua로 Redis 잔액 사전 검증 → DB 비관적락으로 실제 차감
    private static final String MINUS_SCRIPT =
            "local b = tonumber(redis.call('GET', KEYS[1]))\n" +
                    "if b == nil then return -1 end\n" +
                    "if b < tonumber(ARGV[1]) then return -2 end\n" +
                    "return redis.call('DECRBY', KEYS[1], ARGV[1])";

    // 주문
    // 포인트 차감은 루아스크립트
    public OrderDto orderLua(OrderRequest request) {
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
            Menu menu = menuRepository.findById(menuId)
                    .filter(m -> m.getDeletedAt() == null)
                    .orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));
        }

        // Lua: Redis로 잔액 사전 검증, 차감
        Long luaResult = stringRedisTemplate.execute(
                new DefaultRedisScript<>(MINUS_SCRIPT, Long.class),
                List.of("point:balance:" + userId),
                String.valueOf(totalPrice)
        );

        // 잔액을 읽어서 값이 없으면 -1(캐시에없다), 값이 차감할 금액보다 적으면 -2, 아니면 차감
        if (luaResult == null || luaResult == -1L) {
            validatePointFromDb(userId, totalPrice); // 캐시 미스 → DB fallback
        } else if (luaResult == -2L) {
            throw new ServiceErrorException(POINT_INSUFFICIENT);
        }

        log.info("[Lua] Redis 잔액 검증 결과 - userId={}, result={}", userId, luaResult);

        // 5. 주문 저장
        Order order = Order.createOrder(userId, cartId, totalPrice);
        Order savedOrder = orderRepository.save(order);

        // 6. 포인트 차감
        minusPointFromDb(userId, totalPrice, savedOrder.getId());

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

        log.info("[주문-Lua] 상태: 준비중 orderId={}, userId={}, price={}",
                savedOrder.getId(), userId, totalPrice);

        // 8. 주문 완료 후 카트 삭제
        cartService.deleteCartAfterOrder(cartId);

        List<CartItemDto> itemDtos = cartItems.stream().map(CartItemDto::from).toList();

        return new OrderDto(savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getCartId(), itemDtos, savedOrder.getPrice(), OrderStatus.PREPARING);
    }

    // Redis 캐시 미스 시 DB에서 잔액만 확인
    private void validatePointFromDb(Long userId, int price) {
        Point point = pointRepository.findByCustomerId(userId)
                .orElseThrow(() -> new ServiceErrorException(NOT_FOUND_POINT));
        if (point.getBalance() < price)
            throw new ServiceErrorException(POINT_INSUFFICIENT);
    }

    // DB 비관적 락으로 포인트 차감 + Redis 캐시 최신화
    private void minusPointFromDb(Long userId, int price, Long orderId) {
        Point point = pointRepository.findByCustomerIdWithPes(userId)
                .orElseThrow(() -> new ServiceErrorException(NOT_FOUND_POINT));
        point.use(price);
        historyRepository.save(History.createUseHistory(userId, price, orderId));
        // DB 차감 후 Redis 캐시 최신화

        // 기존 레디스템플릿 사용시 시리얼라이저 "" 파싱 문제가 있어서 스트링레디스템플릿 사용
        stringRedisTemplate.opsForValue().set(BALANCE_KEY_PREFIX + userId, String.valueOf(point.getBalance()));
        log.info("[주문-Lua] DB 포인트 차감 완료 - userId={}, balance={}", userId, point.getBalance());
    }
}
