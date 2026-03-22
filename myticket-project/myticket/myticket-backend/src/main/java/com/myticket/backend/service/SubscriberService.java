package com.myticket.backend.service;

import com.myticket.backend.model.Subscriber;
import com.myticket.backend.repository.SubscriberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    public SubscriberService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    @Transactional
    public Subscriber subscribe(String email) {
        if (subscriberRepository.existsByEmail(email)) {
            throw new IllegalStateException("Already subscribed");
        }
        Subscriber subscriber = Subscriber.builder()
                .email(email)
                .unsubscribeToken(UUID.randomUUID().toString())
                .build();
        return subscriberRepository.save(subscriber);
    }

    @Transactional
    public void unsubscribe(String email, String token) {
        Subscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Subscriber not found"));
        if (!subscriber.getUnsubscribeToken().equals(token)) {
            throw new RuntimeException("Invalid unsubscribe token");
        }
        subscriberRepository.delete(subscriber);
    }

    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }
}
