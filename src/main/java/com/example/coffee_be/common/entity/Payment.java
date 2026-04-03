package com.example.coffee_be.common.entity;


import com.example.coffee_be.domain.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, unique = true)
    private String paymentId; // 포트원 paymentId

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Version
    private Long version;

    public static Payment create(Long customerId, String paymentId, int amount) {
        Payment payment = new Payment();
        payment.customerId = customerId;
        payment.paymentId = paymentId;
        payment.amount = amount;
        payment.status = PaymentStatus.READY;
        return payment;
    }

    public void paid() {
        if(this.status == PaymentStatus.READY) {
            this.status = PaymentStatus.PAID;
        }
    }

    public void failed() {
        if (this.status == PaymentStatus.READY) {
            this.status = PaymentStatus.FAILED;
        }
    }

    public void cancelled() {
        if (this.status == PaymentStatus.PAID) {
            this.status = PaymentStatus.CANCELLED;
        }
    }
}
