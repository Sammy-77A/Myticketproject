package com.myticket.backend.service;

import com.myticket.backend.model.TicketTier;
import com.myticket.backend.repository.TicketTierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EarlyBirdExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(EarlyBirdExpiryJob.class);

    private final TicketTierRepository tierRepository;

    public EarlyBirdExpiryJob(TicketTierRepository tierRepository) {
        this.tierRepository = tierRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireEarlyBirdTiers() {
        List<TicketTier> tiers = tierRepository.findByIsEarlyBirdTrueAndIsExpiredFalseAndClosesAtBefore(LocalDateTime.now());
        for (TicketTier tier : tiers) {
            tier.setExpired(true);
            tierRepository.save(tier);
            log.info("Early bird tier {} for event {} has expired (closesAt={})",
                    tier.getId(), tier.getEvent().getId(), tier.getClosesAt());
        }
    }
}
