package com.finsimx.exception;

import lombok.Getter;

@Getter
public class OrderException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public OrderException(String errorCode, int statusCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
