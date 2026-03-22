package com.myticket.common.exception;

public class ReturnWindowClosedException extends RuntimeException {
    public ReturnWindowClosedException(String message) {
        super(message);
    }
}
