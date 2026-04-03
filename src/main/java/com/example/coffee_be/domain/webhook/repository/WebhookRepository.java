package com.example.coffee_be.domain.webhook.repository;

import com.example.coffee_be.common.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    boolean existsByWebhookId(String webhookId);
}
