package com.myticket.backend.service;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.TicketTier;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.TicketTierRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.common.dto.PaymentResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "offline", matchIfMissing = true)
public class MockPaymentService implements PaymentService {

    private final EventRepository eventRepository;
    private final TicketTierRepository tierRepository;
    private final UserRepository userRepository;
    
    // Store mock transactions: txnId -> MockDetails
    public static final Map<String, MockDetails> MOCK_TXNS = new ConcurrentHashMap<>();
    
    public static class MockDetails {
        public String txnId;
        public double amount;
        public String eventTitle;
        public String phone;
        public boolean confirmed;
    }

    public MockPaymentService(EventRepository eventRepository, TicketTierRepository tierRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PaymentResponse initiatePayment(Long eventId, Long tierId, int quantity, String customerEmail) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        TicketTier tier = tierRepository.findById(tierId).orElseThrow();
        User user = userRepository.findByEmail(customerEmail).orElseThrow();
        
        String txnId = "MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8);
        double amount = tier.getPrice() * quantity;
        
        MockDetails details = new MockDetails();
        details.txnId = txnId;
        details.amount = amount;
        details.eventTitle = event.getTitle();
        details.phone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "0712345678";
        details.confirmed = false;
        
        MOCK_TXNS.put(txnId, details);
        
        return PaymentResponse.builder()
                .redirectUrl("/stk-waiting.html?txn=" + txnId)
                .txnId(txnId)
                .build();
    }

    @Override
    public boolean verifyPayment(String txnId) {
        // Mock payment always returns true for any MOCK-TXN-* id 
        // to simplify the flow in offline testing
        MockDetails details = MOCK_TXNS.get(txnId);
        if (details != null) {
            return details.confirmed;
        }
        return true;
    }
}
