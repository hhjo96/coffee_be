package com.example.coffee_be.domain.payment.model.dto;

public record ConfirmPaymentDto(
        Long paymentDbId,
        String paymentId,
        Long customerId,
        int amount,
        String status
) {}
