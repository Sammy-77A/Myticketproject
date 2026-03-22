package com.myticket.backend.service;

import com.myticket.common.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse initiatePayment(Long eventId, Long tierId, int quantity, String customerEmail);
    boolean verifyPayment(String txnId);
}
