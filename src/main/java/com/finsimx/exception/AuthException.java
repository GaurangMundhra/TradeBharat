package com.finsimx.exception;

public class AuthException extends RuntimeException {

    private String errorCode;
    private int statusCode;

    public AuthException(String message) {
        super(message);
        this.statusCode = 401;
        this.errorCode = "AUTH_ERROR";
    }

    public AuthException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = 401;
    }

    public AuthException(String message, String errorCode, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
