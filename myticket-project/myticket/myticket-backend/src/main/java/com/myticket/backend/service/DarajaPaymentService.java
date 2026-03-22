package com.myticket.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myticket.backend.model.Event;
import com.myticket.backend.model.TicketTier;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.TicketTierRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.common.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "online")
public class DarajaPaymentService implements PaymentService {

    @Value("${daraja.consumer-key}")
    private String consumerKey;
    
    @Value("${daraja.consumer-secret}")
    private String consumerSecret;
    
    @Value("${daraja.shortcode}")
    private String shortcode;
    
    @Value("${daraja.passkey}")
    private String passkey;
    
    @Value("${daraja.callback-url}")
    private String callbackUrl;
    
    @Value("${daraja.base-url}")
    private String baseUrl;

    private final EventRepository eventRepository;
    private final TicketTierRepository tierRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    private String cachedAccessToken;
    private LocalDateTime tokenExpiry;

    public DarajaPaymentService(EventRepository eventRepository, TicketTierRepository tierRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    private synchronized String getAccessToken() {
        if (cachedAccessToken != null && tokenExpiry != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return cachedAccessToken;
        }

        String auth = consumerKey + ":" + consumerSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = baseUrl + "/oauth/v1/generate?grant_type=client_credentials";
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode body = response.getBody();
            cachedAccessToken = body.path("access_token").asText();
            int expiresIn = body.path("expires_in").asInt(3599);
            tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 60);
            return cachedAccessToken;
        } else {
            throw new RuntimeException("Failed to get Daraja access token: " + response.getStatusCode());
        }
    }

    @Override
    public PaymentResponse initiatePayment(Long eventId, Long tierId, int quantity, String customerEmail) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        TicketTier tier = tierRepository.findById(tierId).orElseThrow();
        User user = userRepository.findByEmail(customerEmail).orElseThrow();

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            throw new RuntimeException("User does not have a registered phone number for M-Pesa");
        }
        String phone = user.getPhoneNumber().trim();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String passwordStr = shortcode + passkey + timestamp;
        String password = Base64.getEncoder().encodeToString(passwordStr.getBytes(StandardCharsets.UTF_8));
        
        long amount = Math.round(tier.getPrice() * quantity);

        Map<String, Object> body = new HashMap<>();
        body.put("BusinessShortCode", shortcode);
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("TransactionType", "CustomerPayBillOnline");
        body.put("Amount", amount);
        body.put("PartyA", phone);
        body.put("PartyB", shortcode);
        body.put("PhoneNumber", phone);
        body.put("CallBackURL", callbackUrl);
        body.put("AccountReference", "MyTicket-" + eventId);
        body.put("TransactionDesc", "Ticket payment for " + event.getTitle());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = baseUrl + "/mpesa/stkpush/v1/processrequest";
        
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode resBody = response.getBody();
            String checkoutRequestId = resBody.path("CheckoutRequestID").asText();
            
            return PaymentResponse.builder()
                    .redirectUrl("/stk-waiting?txn=" + checkoutRequestId)
                    .txnId(checkoutRequestId)
                    .build();
        } else {
            throw new RuntimeException("Failed to initiate STK Push");
        }
    }

    @Override
    public boolean verifyPayment(String txnId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String passwordStr = shortcode + passkey + timestamp;
        String password = Base64.getEncoder().encodeToString(passwordStr.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("BusinessShortCode", shortcode);
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("CheckoutRequestID", txnId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = baseUrl + "/mpesa/stkpush/v1/query";

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                int resultCode = response.getBody().path("ResultCode").asInt(-1);
                return resultCode == 0;
            }
        } catch (Exception e) {
            // Daraja usually returns 400 for pending/failed
        }
        return false;
    }
}
