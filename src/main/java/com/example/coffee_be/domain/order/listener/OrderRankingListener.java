package com.example.coffee_be.domain.order.listener;

import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import static com.example.coffee_be.common.model.kafka.topic.KafkaTopics.TOPIC_ORDER_COMPLETED;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderRankingListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String RANKING_KEY = "ranking:menus";

    @KafkaListener(
        topics = TOPIC_ORDER_COMPLETED,
        groupId = "menu-ranking-group",
        containerFactory = "menuRankingKafkaListenerContainerFactory"
    )
    public void consume(OrderCompletedEvent event) {

        log.info("[Consumer-Record] 주문 완료 이벤트 수신 - orderId={}, userId={}", event.getOrderId(), event.getUserId());

        for (OrderCompletedEvent.OrderItemEvent item : event.getItems()) {
            redisTemplate.opsForZSet()
                    .incrementScore(RANKING_KEY,
                            item.getMenuId().toString(),
                            item.getQuantity());
            log.info("[랭킹 업데이트] menuId={}, +{}점",
                    item.getMenuId(), item.getQuantity());
        }
    }
}
