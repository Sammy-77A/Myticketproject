package com.myticket.common.exception;

public class ClaimExpiredException extends RuntimeException {
    public ClaimExpiredException(String message) {
        super(message);
    }
}
