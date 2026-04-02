package com.example.coffee_be.domain.payment.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PreparePaymentRequest {

    @NotNull(message = "사용자번호는 필수입니다")
    private Long customerId;

    @NotNull(message = "충전 금액은 필수입니다")
    private int amount;
}
