package com.example.coffee_be.domain.payment.listener;

import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.common.model.kafka.event.PaymentCompletedEvent;
import com.example.coffee_be.common.model.kafka.event.PointChargedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.example.coffee_be.common.model.kafka.topic.KafkaTopics.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentListener {

    // 결제완료 컨슈머
    @KafkaListener(
        topics = TOPIC_PAYMENT_COMPLETED,
        groupId = "payment-history-group",
        containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void consume(PaymentCompletedEvent event) {

        log.info("[결제완료 이벤트 수신] paymentId={}, customerId={}, amount={}",
                event.getPaymentId(), event.getCustomerId(), event.getAmount());

    }


}
