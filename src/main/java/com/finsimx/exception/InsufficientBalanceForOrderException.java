package com.finsimx.exception;

public class InsufficientBalanceForOrderException extends RuntimeException {
    public InsufficientBalanceForOrderException(String message) {
        super(message);
    }
}
