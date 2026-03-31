package com.example.coffee_be.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"success", "code", "message", "data"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    public static <T> BaseResponse<T> success(String code, String message, T data) {
        return new BaseResponse<>(true, code, message, data);
    }

    public static <T> BaseResponse<T> fail(String code, String message, T data) {
        return new BaseResponse<>(false, code, message, data);
    }
}