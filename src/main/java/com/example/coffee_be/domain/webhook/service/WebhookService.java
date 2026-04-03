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
import com.example.coffee_be.domain.webhook.repository.WebhookRepository;
import com.example.coffee_be.domain.webhook.verifier.PortOneSdkWebhookVerifier;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.example.coffee_be.common.exception.ErrorEnum.PAYMENT_NOT_PAID;
import static com.example.coffee_be.common.exception.ErrorEnum.PAYMENT_VERIFY_FAILED;

// 웹훅 자체를 처리하는 매서드와 웹훅을 받아서 비즈니스로직 처리하는 매서드로 나눔

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WebhookService {

    private final PortOneSdkWebhookVerifier verifier;
    private final PortOneClient portOneClient;
    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final WebhookRepository webhookRepository;
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    // 서명 검증 + 역직렬화 (컨트롤러에서 호출)
    public Webhook verifyAndParse(String rawBody, String id, String sig, String ts) throws WebhookVerificationException {
        return verifier.verify(rawBody, id, sig, ts);
    }

    // 중복수신방지, 웹훅 저장, 성공/실패 업데이트
    public void process(Webhook webhook, String webhookId) {
        if (webhook instanceof WebhookTransaction transaction) {
            WebhookTransactionData data = transaction.getData();
            log.info(
                    "[웹훅] VERIFIED Transaction paymentId={} transactionId={}  timestamp={}",
                    data.getPaymentId(),
                    data.getTransactionId(),
                    transaction.getTimestamp()
            );

            String paymentId = data.getPaymentId();

            // 중복수신방지
            if (webhookRepository.existsByWebhookId(webhookId)) {
                log.info("[웹훅] 중복 수신 스킵 - webhookId={}", webhookId);
                return;
            }

            // 저장
            com.example.coffee_be.common.entity.Webhook savedWebhook =
            webhookRepository.save(com.example.coffee_be.common.entity.Webhook.create(webhookId, paymentId));

            try {
                if (webhook instanceof WebhookTransactionPaid) {
                    handlePaid(paymentId);                          // 결제 완료 -> 다음 처리
                } else {
                    log.info("[웹훅] 기타 타입 - {}", webhook.getClass().getSimpleName());
                }
                savedWebhook.processed();

            } catch (Exception e) {
                savedWebhook.failed();     // FAILED 로 업데이트
                log.error("[웹훅] 처리 실패 - webhookId={}", webhookId, e);
                throw e;
            }
        }

    }

    // 결제 객체 저장, 포인트 충전, 카프카이벤트 발행
    private void handlePaid(String paymentId) {

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
        try {
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
        } catch (ObjectOptimisticLockingFailureException e) {
            // confirm()이 동시에 먼저 커밋한 것 → 정상 케이스, 스킵
            log.info("[웹훅] 결제가 먼저 처리 완료 - paymentId={}", paymentId);
        }
    }
}
