package com.example.coffee_be.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static com.example.coffee_be.common.constants.Constants.*;

@Getter
public enum ErrorEnum {

    // 경매 관련
    INVALID_MINIMUM_BID(HttpStatus.BAD_REQUEST, MSG_NOT_FOUND_MEMBER),
    INVALID_MINIMUM_BID2(HttpStatus.BAD_REQUEST, MSG_NOT_FOUND_MEMBER);

    private final HttpStatus status;
    private final String message;

    ErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
