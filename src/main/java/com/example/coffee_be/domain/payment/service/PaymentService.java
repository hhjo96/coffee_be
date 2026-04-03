package com.example.coffee_be.domain.payment.service;


import com.example.coffee_be.common.entity.Payment;
import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.common.external.PortOneClient;
import com.example.coffee_be.common.external.PortOnePaymentResponse;
import com.example.coffee_be.common.model.kafka.event.PaymentCompletedEvent;
import com.example.coffee_be.common.model.kafka.topic.KafkaTopics;
import com.example.coffee_be.domain.payment.enums.PaymentStatus;
import com.example.coffee_be.domain.payment.model.dto.ConfirmPaymentDto;
import com.example.coffee_be.domain.payment.model.dto.PreparePaymentDto;
import com.example.coffee_be.domain.payment.model.request.ConfirmPaymentRequest;
import com.example.coffee_be.domain.payment.model.request.PreparePaymentRequest;
import com.example.coffee_be.domain.payment.repository.PaymentRepository;
import com.example.coffee_be.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.example.coffee_be.common.exception.ErrorEnum.PAYMENT_VERIFY_FAILED;

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

        paymentRepository.save(Payment.create(request.getCustomerId(), paymentId, request.getAmount()));

        return new PreparePaymentDto(
                paymentId, request.getCustomerId(), request.getAmount());
    }

    // 2. 결제 검증 + Payment 저장 + 포인트 충전
    @Transactional
    public ConfirmPaymentDto confirm(ConfirmPaymentRequest request) {

        // prepare에서 READY로 저장한 Payment 조회
        Payment payment = paymentRepository.findByPaymentId(request.getPaymentId())
                .orElseThrow(() -> new ServiceErrorException(PAYMENT_VERIFY_FAILED));

        // 웹훅이 먼저 처리했으면 스킵
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("[결제확인] 이미 처리된 결제 - paymentId={}", request.getPaymentId());
            return new ConfirmPaymentDto(payment.getId(), payment.getPaymentId(),
                    payment.getCustomerId(), payment.getAmount(), "PAID");
        }

        // PortOne 검증
        PortOnePaymentResponse portOnePayment;
        try {
            portOnePayment = portOneClient.getPayment(request.getPaymentId());
        } catch (Exception e) {
            throw new ServiceErrorException(PAYMENT_VERIFY_FAILED);
        }
        if (!"PAID".equals(portOnePayment.status()))
            throw new ServiceErrorException(ErrorEnum.PAYMENT_NOT_PAID);
        if (portOnePayment.amount().total().compareTo(BigDecimal.valueOf(request.getAmount())) != 0)
            throw new ServiceErrorException(ErrorEnum.PAYMENT_AMOUNT_MISMATCH);

        //결제 상태를 결제 완료로 수정(낙관적 락)
        try {
            payment.paid();

            // 포인트 충전 (paymentId 연결, 트랜잭션으로 묶임)
            pointService.chargePointWithPayment(request.getCustomerId(), request.getAmount(), payment.getId());

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(payment.getId())
                    .customerId(payment.getCustomerId())
                    .amount(payment.getAmount())
                    .paidAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            // 카프카 PAYMENTCOMPLETED이벤트발행
            kafkaTemplate.send(KafkaTopics.TOPIC_PAYMENT_COMPLETED,
                    String.valueOf(payment.getId()), event);

        } catch (ObjectOptimisticLockingFailureException e) {
            // 웹훅이 동시에 먼저 커밋한 것 → 정상 케이스
            log.info("[결제확인] 웹훅이 먼저 처리 완료 - paymentId={}", request.getPaymentId());
            Payment latest = paymentRepository.findByPaymentId(request.getPaymentId())
                    .orElseThrow(() -> new ServiceErrorException(PAYMENT_VERIFY_FAILED));
            return new ConfirmPaymentDto(latest.getId(), latest.getPaymentId(),
                    latest.getCustomerId(), latest.getAmount(), "PAID");
        }

        log.info("[결제+포인트충전 완료] paymentId={}, customerId={}, amount={}",
                request.getPaymentId(), request.getCustomerId(), request.getAmount());

        return new ConfirmPaymentDto(
                payment.getId(), payment.getPaymentId(),
                payment.getCustomerId(), payment.getAmount(), "PAID");
    }
}
