package com.example.coffee_be.domain.payment.controller;


import com.example.coffee_be.common.response.BaseResponse;
import com.example.coffee_be.domain.payment.model.dto.ConfirmPaymentDto;
import com.example.coffee_be.domain.payment.model.dto.PreparePaymentDto;
import com.example.coffee_be.domain.payment.model.request.ConfirmPaymentRequest;
import com.example.coffee_be.domain.payment.model.request.PreparePaymentRequest;
import com.example.coffee_be.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 준비 - paymentId 발급
    @PostMapping("/prepare")
    public ResponseEntity<BaseResponse<PreparePaymentDto>> prepare(
            @Valid @RequestBody PreparePaymentRequest request) {
        return ResponseEntity.ok()
                .body(BaseResponse.success("200", "결제 준비 완료",
                        paymentService.prepare(request)));
    }

    // 결제 검증 + 포인트 충전
    @PostMapping("/confirm")
    public ResponseEntity<BaseResponse<ConfirmPaymentDto>> confirm(
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok()
                .body(BaseResponse.success("200", "결제 및 포인트 충전 완료",
                        paymentService.confirm(request)));
    }
}