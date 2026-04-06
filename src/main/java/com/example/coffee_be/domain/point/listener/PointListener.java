package com.example.coffee_be.domain.point.listener;

import com.example.coffee_be.common.model.kafka.event.PaymentCompletedEvent;
import com.example.coffee_be.common.model.kafka.event.PointChargedEvent;
import com.example.coffee_be.common.model.kafka.event.PointUsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.example.coffee_be.common.model.kafka.topic.KafkaTopics.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class PointListener {

    // 포인트충전 컨슈머
    @KafkaListener(
            topics = TOPIC_POINT_CHARGED,
            groupId = "point-charged-group",
            containerFactory = "pointChargedKafkaListenerContainerFactory"
    )
    public void consumePointCharged(PointChargedEvent event) {
        log.info("[포인트충전 이벤트 수신] userId={}, amount={}",
                event.getUserId(), event.getAmount());
    }

    // 포인트사용 컨슈머
    @KafkaListener(
            topics = TOPIC_POINT_USED,
            groupId = "point-used-group",
            containerFactory = "pointUsedKafkaListenerContainerFactory"
    )
    public void consumePointUsed(PointUsedEvent event) {
        log.info("[포인트사용 이벤트 수신] userId={}, orderId= {}, amount={}",
                event.getUserId(), event.getOrderId(), event.getAmount());
    }
}
