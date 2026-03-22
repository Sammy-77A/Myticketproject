package com.myticket.common.exception;

public class TicketSoldOutException extends RuntimeException {
    public TicketSoldOutException(String message) {
        super(message);
    }
}
