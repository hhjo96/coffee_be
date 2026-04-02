package com.example.coffee_be.common.entity;

import com.example.coffee_be.domain.point.enums.PointStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "histories"
        //,
        //   indexes = {
        //           @Index(name = "idx_auction_start", columnList = "start_at DESC, id DESC")
        //   }
)
@Getter
@NoArgsConstructor
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    private PointStatus status;

    private LocalDateTime payedAt;

    private Long paymentId; // 우리쪽 결제아이디

    private Long orderId;

    public static History createChargeHistory(Long customerId, int amount, Long paymentId) {
        History h = new History();
        h.customerId = customerId;
        h.amount = amount;
        h.status = PointStatus.CHARGED;
        h.payedAt = LocalDateTime.now();
        h.paymentId = paymentId;
        return h;
    }

    public static History createUseHistory(Long customerId, int amount, Long orderId) {
        History h = new History();
        h.customerId = customerId;
        h.amount = amount;
        h.status = PointStatus.USED;
        h.payedAt = LocalDateTime.now();
        h.orderId = orderId;
        return h;
    }
}
