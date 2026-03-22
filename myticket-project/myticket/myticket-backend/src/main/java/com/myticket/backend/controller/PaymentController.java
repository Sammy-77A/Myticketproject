package com.myticket.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.myticket.backend.model.Ticket;
import com.myticket.backend.repository.TicketRepository;
import com.myticket.backend.service.MockPaymentService;
import com.myticket.backend.service.TicketService;
import com.myticket.common.enums.TicketStatus;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final Environment env;
    
    public PaymentController(TicketService ticketService, TicketRepository ticketRepository, Environment env) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.env = env;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> darajaCallback(@RequestBody JsonNode payload) {
        try {
            JsonNode stkCallback = payload.path("Body").path("stkCallback");
            int resultCode = stkCallback.path("ResultCode").asInt(-1);
            String checkoutRequestId = stkCallback.path("CheckoutRequestID").asText();
            
            if (resultCode == 0) {
                ticketService.confirmBooking(checkoutRequestId);
            } else {
                ticketService.cancelPendingTickets(checkoutRequestId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stk-details")
    public ResponseEntity<Map<String, Object>> getStkDetails(@RequestParam("txn") String txnId) {
        String mode = env.getProperty("app.mode", "offline");
        
        if ("offline".equalsIgnoreCase(mode)) {
            MockPaymentService.MockDetails details = MockPaymentService.MOCK_TXNS.get(txnId);
            if (details != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("amount", details.amount);
                map.put("phone", details.phone);
                map.put("eventTitle", details.eventTitle);
                return ResponseEntity.ok(map);
            }
        }
        
        List<Ticket> pending = ticketRepository.findByPaymentTxnId(txnId);
        if (pending.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Ticket t = pending.get(0);
        
        Map<String, Object> map = new HashMap<>();
        map.put("amount", t.getTier().getPrice() * pending.size());
        map.put("phone", t.getUser().getPhoneNumber());
        map.put("eventTitle", t.getEvent().getTitle());
        return ResponseEntity.ok(map);
    }
    
    @GetMapping("/mock-details")
    public ResponseEntity<Map<String, Object>> getMockDetails(@RequestParam("txn") String txnId) {
        return getStkDetails(txnId);
    }
    
    @PostMapping("/mock-confirm")
    public ResponseEntity<Void> confirmMockPayment(@RequestParam("txn") String txnId) {
        String mode = env.getProperty("app.mode", "offline");
        if ("online".equalsIgnoreCase(mode)) {
            return ResponseEntity.notFound().build();
        }
        MockPaymentService.MockDetails details = MockPaymentService.MOCK_TXNS.get(txnId);
        if (details != null) {
            details.confirmed = true;
            ticketService.confirmBooking(txnId);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/stk-status")
    public ResponseEntity<Map<String, String>> getStkStatus(@RequestParam("txn") String txnId) {
        List<Ticket> tickets = ticketRepository.findByPaymentTxnId(txnId);
        if (tickets.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "FAILED"));
        }
        
        TicketStatus status = tickets.get(0).getStatus();
        if (status == TicketStatus.BOOKED) {
            return ResponseEntity.ok(Map.of("status", "CONFIRMED"));
        } else if (status == TicketStatus.CANCELLED) {
            return ResponseEntity.ok(Map.of("status", "FAILED"));
        } else {
            return ResponseEntity.ok(Map.of("status", "PENDING"));
        }
    }
}
