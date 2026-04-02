package com.example.coffee_be.common.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneError(
        String type,
        String message
) {
}
