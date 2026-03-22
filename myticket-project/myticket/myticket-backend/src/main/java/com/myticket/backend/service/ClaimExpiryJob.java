package com.myticket.backend.service;

import com.myticket.backend.model.Waitlist;
import com.myticket.backend.repository.WaitlistRepository;
import com.myticket.common.enums.WaitlistStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ClaimExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ClaimExpiryJob.class);

    private final WaitlistRepository waitlistRepository;
    private final WaitlistService waitlistService;
    private final AuditLogService auditLogService;

    public ClaimExpiryJob(WaitlistRepository waitlistRepository, WaitlistService waitlistService, AuditLogService auditLogService) {
        this.waitlistRepository = waitlistRepository;
        this.waitlistService = waitlistService;
        this.auditLogService = auditLogService;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void expireUnclaimedSlots() {
        List<Waitlist> expired = waitlistRepository.findByStatusAndClaimExpiresAtBefore(WaitlistStatus.NOTIFIED, LocalDateTime.now());
        for (Waitlist entry : expired) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
            auditLogService.logAction(entry.getUser().getEmail(), "WAITLIST_EXPIRED", "Waitlist claim expired for event " + entry.getEvent().getId());

            log.info("Expired unclaimed waitlist slot {} for event {}, tier {}",
                    entry.getId(), entry.getEvent().getId(), entry.getTier().getId());

            waitlistService.notifyNextInLine(entry.getEvent().getId(), entry.getTier().getId());
        }
    }
}
