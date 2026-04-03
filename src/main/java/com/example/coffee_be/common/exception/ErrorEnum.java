package com.example.coffee_be.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static com.example.coffee_be.common.constants.Constants.*;

@Getter
public enum ErrorEnum {

    // 메뉴 관련
    NOT_FOUND_MENU(HttpStatus.NOT_FOUND, MSG_NOT_FOUND_MENU),

    // 주문 관련
    ORDER_LOCK_FAILED(HttpStatus.CONFLICT, MSG_ORDER_LOCK_FAILED),

    //포인트 관련
    POINT_CHARGE_UNDER_ZERO(HttpStatus.BAD_REQUEST, MSG_POINT_CHARGE_UNDER_ZERO),
    POINT_USE_UNDER_ZERO(HttpStatus.BAD_REQUEST, MSG_POINT_USE_UNDER_ZERO),
    POINT_INSUFFICIENT(HttpStatus.BAD_REQUEST, MSG_POINT_INSUFFICIENT),
    POINT_CHARGE_CONFLICT(HttpStatus.CONFLICT, MSG_POINT_CHARGE_CONFLICT),
    POINT_LOCK_FAILED(HttpStatus.CONFLICT, MSG_POINT_LOCK_CONFLICT),
    NOT_FOUND_POINT(HttpStatus.NOT_FOUND, MSG_NOT_FOUND_POINT),


    // 카트아이템 관련
    NOT_FOUND_CARTITEM(HttpStatus.NOT_FOUND, MSG_NOT_FOUND_CARTITEM),


    // 카트 관련
    NOT_FOUND_CART(HttpStatus.NOT_FOUND, MSG_NOT_FOUND_CART),
    NOT_CART_OWNER(HttpStatus.BAD_REQUEST, MSG_NOT_CART_OWNER),
    CART_EMPTY(HttpStatus.BAD_REQUEST, MSG_CART_EMPTY),


    // 결제 관련

    PAYMENT_NOT_PAID(HttpStatus.BAD_REQUEST, MSG_PAYMENT_NOT_PAID),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, MSG_PAYMENT_AMOUNT_MISMATCH),
    PAYMENT_VERIFY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, MSG_PAYMENT_VERIFY_FAILED),
    PAYMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, MSG_PAYMENT_NOT_FOUND),


    // 웹훅 관련
    WEBHOOK_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST, MSG_WEBHOOK_SIGNATURE_INVALID)
    ;

    private final HttpStatus status;
    private final String message;

    ErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
