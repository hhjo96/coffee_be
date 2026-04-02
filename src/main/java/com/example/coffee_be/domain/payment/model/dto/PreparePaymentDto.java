package com.example.coffee_be.domain.payment.model.dto;

//결제 시작 전에 paymentID 만들기에 사용
public record PreparePaymentDto(
        String paymentId,
        Long customerId,
        int amount
) {}
