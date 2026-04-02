package com.example.coffee_be.domain.payment.repository;

import com.example.coffee_be.common.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
