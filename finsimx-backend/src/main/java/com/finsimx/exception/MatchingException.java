package com.finsimx.exception;

import lombok.Getter;

@Getter
public class MatchingException extends RuntimeException {

    private final String errorCode;
    private final int statusCode;

    public MatchingException(String errorCode, int statusCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
