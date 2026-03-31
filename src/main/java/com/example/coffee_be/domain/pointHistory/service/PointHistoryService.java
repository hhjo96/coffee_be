package com.example.coffee_be.domain.pointHistory.service;

import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointHistoryService {
    public void savePointHistory(OrderCompletedEvent event) {

        // idempotent 체크


        log.info("[DB] 결제 기록 저장 완료 - paymentId={}, orderId={}, productId={}", event.getPaymentId(), event.getOrderId(), event.getProductId());
    }
}
