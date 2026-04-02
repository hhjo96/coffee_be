package com.example.coffee_be.domain.payment.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 결제준비에서 paymentId만들어주면 그걸로 실제 결제
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmPaymentRequest {

    @NotNull(message = "결제 ID는 필수입니다")
    private String paymentId;

    @NotNull(message = "사용자번호는 필수입니다")
    private Long customerId;

    @NotNull(message = "충전 금액은 필수입니다")
    private int amount;
}
