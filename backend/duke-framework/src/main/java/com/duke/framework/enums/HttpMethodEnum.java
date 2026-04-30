package com.duke.framework.enums;

import lombok.Getter;

@Getter
public enum HttpMethodEnum {

    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    PATCH("PATCH");

    private final String method;

    HttpMethodEnum(String method) {
        this.method = method;
    }
}
