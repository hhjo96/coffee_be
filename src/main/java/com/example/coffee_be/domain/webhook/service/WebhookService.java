package com.example.coffee_be.domain.webhook.service;

import com.example.coffee_be.domain.webhook.repository.WebhookRepository;
import com.example.coffee_be.domain.webhook.verifier.PortOneSdkWebhookVerifier;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// 웹훅을 잘 받았음을 처리

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WebhookService {

    private final PortOneSdkWebhookVerifier verifier;
    private final WebhookPointService webhookPointService;
    private final WebhookRepository webhookRepository;

    // 서명 검증 + 역직렬화 (컨트롤러에서 호출)
    public Webhook verifyAndParse(String rawBody, String id, String sig, String ts) throws WebhookVerificationException {
        return verifier.verify(rawBody, id, sig, ts);
    }

    // 중복수신방지, 웹훅 저장, 성공/실패 업데이트
    public void process(Webhook webhook, String webhookId) {
        // 포트원웹훅이 결제 타입일때만 처리
        if (webhook instanceof WebhookTransaction transaction) {
            WebhookTransactionData data = transaction.getData();
            log.info(
                    "[웹훅] 처리 시작 paymentId={} timestamp={}",
                    data.getPaymentId(),
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
                    webhookPointService.handlePaid(paymentId);

                } else if (webhook instanceof WebhookTransactionFailed) {
                    webhookPointService.handleFailed(paymentId); // Payment FAILED 처리

                } else if (webhook instanceof WebhookTransactionCancelled) {
                    webhookPointService.handleCancelled(paymentId); // Payment CANCELLED 처리

                } else {
                    // Ready 등 처리 불필요한 타입
                    log.info("[웹훅] 기타 타입 - {}", webhook.getClass().getSimpleName());
                }
                savedWebhook.processed(); // 어떤 타입이든 정상 수신됐으면 PROCESSED

            } catch (ObjectOptimisticLockingFailureException e) {
                // confirm()이 먼저 커밋한 것 → 정상 케이스
                log.info("[웹훅] confirm()이 먼저 처리 완료 - paymentId={}", paymentId);
                savedWebhook.processed(); // 웹훅 자체는 정상 처리된 것

            } catch (Exception e) {
                savedWebhook.failed();     // FAILED 로 업데이트
                log.error("[웹훅] 처리 실패 - webhookId={}", webhookId, e);
                throw e;
            }
        }

    }

}
