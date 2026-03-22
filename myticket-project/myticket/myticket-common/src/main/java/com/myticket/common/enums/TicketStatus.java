package com.myticket.common.enums;

/**
 * PENDING = payment initiated but not yet confirmed.
 * Used to hold capacity while awaiting M-Pesa STK Push confirmation.
 */
public enum TicketStatus {
    BOOKED,
    USED,
    CANCELLED,
    TRANSFERRED,
    PENDING
}
