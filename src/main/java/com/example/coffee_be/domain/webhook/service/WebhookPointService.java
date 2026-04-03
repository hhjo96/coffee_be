package com.example.coffee_be.domain.webhook.service;


import com.example.coffee_be.common.entity.Payment;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.common.external.PortOneClient;
import com.example.coffee_be.common.external.PortOnePaymentResponse;
import com.example.coffee_be.common.model.kafka.event.PaymentCompletedEvent;
import com.example.coffee_be.common.model.kafka.topic.KafkaTopics;
import com.example.coffee_be.domain.payment.enums.PaymentStatus;
import com.example.coffee_be.domain.payment.repository.PaymentRepository;
import com.example.coffee_be.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.example.coffee_be.common.exception.ErrorEnum.PAYMENT_NOT_PAID;
import static com.example.coffee_be.common.exception.ErrorEnum.PAYMENT_VERIFY_FAILED;

// 웹훅 후 포인트 충전을 처리

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookPointService {

    private final PortOneClient portOneClient;
    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    // 결제 객체 저장, 포인트 충전, 카프카이벤트 발행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaid(String paymentId) {

        // 이미 paid 상태인지 확인
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ServiceErrorException(PAYMENT_VERIFY_FAILED));

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("[웹훅] 이미 처리된 결제 스킵 - paymentId={}", paymentId);
            return;
        }

        // customerId 확인
        Long customerId = payment.getCustomerId();

        // PortOne API 재조회
        PortOnePaymentResponse res = portOneClient.getPayment(paymentId);
        if (!res.isPaid()) throw new ServiceErrorException(PAYMENT_NOT_PAID);

        // 다 통과했으면 결제완료 상태로 수정(낙관적 락)

        payment.paid();

        int amount = res.amount().total().intValue();

        // 포인트 충전 + PointChargedEvent 발행
        pointService.chargePointWithPayment(customerId, amount, payment.getId());

        // PaymentCompletedEvent 발행
        kafkaTemplate.send(KafkaTopics.TOPIC_PAYMENT_COMPLETED, String.valueOf(payment.getId()),
                PaymentCompletedEvent.builder()
                        .paymentId(payment.getId())
                        .customerId(customerId)
                        .amount(amount)
                        .paidAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build());

        log.info("[웹훅] 결제가 먼저 처리 완료 - paymentId={}", paymentId);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailed(String paymentId) {
        paymentRepository.findByPaymentId(paymentId)
                .ifPresent(payment -> {
                    payment.failed();
                    log.info("[웹훅] 결제 실패 처리 - paymentId={}", paymentId);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCancelled(String paymentId) {
        paymentRepository.findByPaymentId(paymentId)
                .ifPresent(payment -> {
                    payment.cancelled();
                    log.info("[웹훅] 결제 취소 처리 - paymentId={}", paymentId);
                });
    }

}

