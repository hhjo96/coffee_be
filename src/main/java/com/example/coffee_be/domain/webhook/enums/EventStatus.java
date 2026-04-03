package com.example.coffee_be.domain.webhook.enums;

import lombok.Getter;

@Getter
public enum EventStatus {
    RECEIVED,
    PROCESSED,
    FAILED;
}
