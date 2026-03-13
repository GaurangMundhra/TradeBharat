package com.finsimx.exception;

import lombok.Getter;

@Getter
public class SettlementException extends RuntimeException {

    private final String errorCode;
    private final int statusCode;

    public SettlementException(String errorCode, int statusCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
