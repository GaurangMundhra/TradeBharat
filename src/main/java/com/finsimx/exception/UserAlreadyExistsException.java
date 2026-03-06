package com.finsimx.exception;

public class UserAlreadyExistsException extends RuntimeException {

    private String errorCode;
    private int statusCode;

    public UserAlreadyExistsException(String message) {
        super(message);
        this.errorCode = "USER_ALREADY_EXISTS";
        this.statusCode = 409;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
