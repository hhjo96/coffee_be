package com.example.coffee_be.common.entity;

import com.example.coffee_be.domain.webhook.enums.EventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "webhooks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Webhook extends BaseEntity{

    //우리 db에서 관리할 키
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 포트원이랑 통신할때 사용할 키
    @Column(nullable = false, unique = true)
    private String webhookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    private String paymentId;

    private LocalDateTime completedAt;

    public static Webhook create(String webhookId, String paymentId) {
        Webhook webhook = new Webhook();
        webhook.webhookId = webhookId;
        webhook.paymentId = paymentId;
        webhook.status = EventStatus.RECEIVED;
        return webhook;
    }

    public void processed() {
        this.status = EventStatus.PROCESSED;
        this.completedAt = LocalDateTime.now();
    }

    public void failed() {
        this.status = EventStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}
