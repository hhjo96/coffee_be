package com.example.coffee_be.domain.payment.repository;

import com.example.coffee_be.common.entity.Payment;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByPaymentId(String paymentId);

    Optional<Payment> findByPaymentId(@NotNull(message = "결제 ID는 필수입니다") String paymentId);
}
