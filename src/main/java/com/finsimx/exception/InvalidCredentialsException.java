package com.finsimx.exception;

public class InvalidCredentialsException extends RuntimeException {

    private String errorCode;
    private int statusCode;

    public InvalidCredentialsException(String message) {
        super(message);
        this.errorCode = "INVALID_CREDENTIALS";
        this.statusCode = 401;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
