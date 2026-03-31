package com.example.coffee_be.domain.pointHistory.listener;

import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.domain.pointHistory.service.PointHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkaredisspring.common.model.kafka.event.PaymentCompletedEvent;
import org.example.kafkaredisspring.domain.paymentHistory.service.PaymentHistoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.example.coffee_be.common.model.kafka.topic.KafkaTopics.TOPIC_ORDER_COMPLETED;
import static org.example.kafkaredisspring.common.model.kafka.topic.KafkaTopics.TOPIC_PAYMENT_COMPLETED;

@Component
@Slf4j
@RequiredArgsConstructor
public class PointHistoryListener {

    private final PointHistoryService pointHistoryService;

    @KafkaListener(
        topics = TOPIC_ORDER_COMPLETED,
        groupId = "payment-history-group",
        containerFactory = "paymentHistoryKafkaListenerContainerFactory"
    )
    public void consume(OrderCompletedEvent event) {

        log.info("[Consumer-Record] 결제 완료 이벤트 수신 - paymentId={}, orderId={}, userId={}", event.getPaymentId(), event.getOrderId(), event.getUserId());

        pointHistoryService.savePointHistory(event);
    }
}
