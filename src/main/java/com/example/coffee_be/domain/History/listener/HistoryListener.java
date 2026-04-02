package com.example.coffee_be.domain.History.listener;

import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.domain.History.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.example.coffee_be.common.model.kafka.topic.KafkaTopics.TOPIC_ORDER_COMPLETED;

@Component
@Slf4j
@RequiredArgsConstructor
public class HistoryListener {

    @KafkaListener(
        topics = TOPIC_ORDER_COMPLETED,
        groupId = "order-history-group",
        containerFactory = "orderHistoryKafkaListenerContainerFactory"
    )
    public void consume(OrderCompletedEvent event) {

        log.info("[Consumer-Record] 주문 완료 이벤트 수신 - orderId={}, userId={}", event.getOrderId(), event.getUserId());
    }
}
