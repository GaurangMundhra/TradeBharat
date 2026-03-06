package com.finsimx.exception;

public class UserNotFoundException extends RuntimeException {

    private String errorCode;
    private int statusCode;

    public UserNotFoundException(String message) {
        super(message);
        this.errorCode = "USER_NOT_FOUND";
        this.statusCode = 404;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
