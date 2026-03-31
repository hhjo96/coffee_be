package com.example.coffee_be.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ServiceErrorException extends RuntimeException {
    private final HttpStatus httpStatus;

    public ServiceErrorException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.httpStatus = errorEnum.getStatus();
    }
}
