package com.example.coffee_be.domain.payment.service;


import com.example.coffee_be.common.entity.Payment;
import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.common.external.PortOneClient;
import com.example.coffee_be.common.external.PortOnePaymentResponse;
import com.example.coffee_be.common.model.kafka.event.PaymentCompletedEvent;
import com.example.coffee_be.common.model.kafka.topic.KafkaTopics;
import com.example.coffee_be.domain.payment.model.dto.ConfirmPaymentDto;
import com.example.coffee_be.domain.payment.model.dto.PreparePaymentDto;
import com.example.coffee_be.domain.payment.model.request.ConfirmPaymentRequest;
import com.example.coffee_be.domain.payment.model.request.PreparePaymentRequest;
import com.example.coffee_be.domain.payment.repository.PaymentRepository;
import com.example.coffee_be.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PortOneClient portOneClient;
    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    // 1. 결제 준비 - paymentId 생성해서 프론트에 반환
    public PreparePaymentDto prepare(PreparePaymentRequest request) {
        String paymentId = "payment-" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        log.info("[결제준비] paymentId={}, customerId={}, amount={}",
                paymentId, request.getCustomerId(), request.getAmount());

        return new PreparePaymentDto(
                paymentId, request.getCustomerId(), request.getAmount());
    }

    // 2. 결제 검증 + Payment 저장 + 포인트 충전
    @Transactional
    public ConfirmPaymentDto confirm(ConfirmPaymentRequest request) {
        PortOnePaymentResponse portOnePayment;
        try {
            portOnePayment = portOneClient.getPayment(request.getPaymentId());
        } catch (Exception e) {
            log.error("[결제검증 실패] paymentId={}", request.getPaymentId(), e);
            throw new ServiceErrorException(ErrorEnum.PAYMENT_VERIFY_FAILED);
        }

        // status 확인
        if (!"PAID".equals(portOnePayment.status())) {
            throw new ServiceErrorException(ErrorEnum.PAYMENT_NOT_PAID);
        }

        // 금액 검증
        if (portOnePayment.amount().total().compareTo(BigDecimal.valueOf(request.getAmount())) != 0) {
            throw new ServiceErrorException(ErrorEnum.PAYMENT_AMOUNT_MISMATCH);
        }

        // Payment 저장
        Payment payment = Payment.create(request.getCustomerId(), request.getPaymentId(), request.getAmount());
        Payment savedPayment = paymentRepository.save(payment);


        // 포인트 충전 (paymentId 연결, 트랜잭션으로 묶임)
        pointService.chargePointWithPayment(request.getCustomerId(), request.getAmount(), savedPayment.getId());

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(savedPayment.getId())
                .customerId(savedPayment.getCustomerId())
                .amount(savedPayment.getAmount())
                .paidAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        kafkaTemplate.send(KafkaTopics.TOPIC_PAYMENT_COMPLETED,
                String.valueOf(savedPayment.getId()), event);

        log.info("[결제+포인트충전 완료] paymentId={}, customerId={}, amount={}",
                request.getPaymentId(), request.getCustomerId(), request.getAmount());

        return new ConfirmPaymentDto(
                savedPayment.getId(), savedPayment.getPaymentId(),
                savedPayment.getCustomerId(), savedPayment.getAmount(), "PAID");
    }
}
