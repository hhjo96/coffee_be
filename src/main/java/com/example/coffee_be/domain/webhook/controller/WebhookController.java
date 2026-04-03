package com.example.coffee_be.domain.webhook.controller;

import com.example.coffee_be.domain.webhook.service.WebhookService;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader("webhook-id")        String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestBody String rawBody
    ) {
        try {
            // 1단계: 서명 검증 + 역직렬화 (실패 → 401)
            Webhook webhook = webhookService
                    .verifyAndParse(rawBody, webhookId, webhookSignature, webhookTimestamp);

            // 2단계: 타입별 비즈니스 처리
            webhookService.process(webhook, webhookId);

        } catch (WebhookVerificationException e) {
            log.warn("[웹훅] 서명 검증 실패 - id={}", webhookId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok().build();
    }
}
