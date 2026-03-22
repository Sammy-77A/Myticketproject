package com.myticket.backend.service;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.TicketTier;
import com.myticket.backend.repository.TicketTierRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TicketTierService {

    private final TicketTierRepository ticketTierRepository;
    private final AuditLogService auditLogService;

    public TicketTierService(TicketTierRepository ticketTierRepository, AuditLogService auditLogService) {
        this.ticketTierRepository = ticketTierRepository;
        this.auditLogService = auditLogService;
    }

    public String getDealScore(TicketTier tier, Event event) {
        long daysToEvent = ChronoUnit.DAYS.between(LocalDate.now(), event.getEventDate().toLocalDate());
        
        if (daysToEvent <= 3 || (tier.getCapacity() - tier.getTicketsSold()) <= 5) {
            return "CLOSING_SOON";
        }
        
        // Find minimum price among all tiers for this event
        List<TicketTier> allTiers = ticketTierRepository.findByEventId(event.getId());
        double minPrice = allTiers.stream().mapToDouble(TicketTier::getPrice).min().orElse(Double.MAX_VALUE);
        
        if (tier.getPrice() == minPrice) {
            return "BEST_VALUE";
        }
        
        return "STANDARD";
    }

    public boolean isEarlyBirdExpired(TicketTier tier) {
        return tier.isEarlyBird() && tier.getClosesAt() != null && tier.getClosesAt().isBefore(LocalDateTime.now());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireEarlyBirdTiers() {
        LocalDateTime now = LocalDateTime.now();
        List<TicketTier> expiredTiers = ticketTierRepository.findByIsEarlyBirdTrueAndIsExpiredFalseAndClosesAtBefore(now);
        
        for (TicketTier tier : expiredTiers) {
            tier.setExpired(true);
            ticketTierRepository.save(tier);
            
            auditLogService.log(
                    null, 
                    "SYSTEM", 
                    "EARLY_BIRD_EXPIRED", 
                    "TicketTier", 
                    tier.getId(), 
                    "Tier " + tier.getName() + " expired at " + now
            );
        }
        
        if (!expiredTiers.isEmpty()) {
            System.out.println("Expired " + expiredTiers.size() + " early bird tiers.");
        }
    }
}
