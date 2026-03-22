package com.myticket.backend.service;

import com.myticket.backend.model.*;
import com.myticket.backend.repository.*;
import com.myticket.common.dto.BookingRequest;
import com.myticket.common.dto.WaitlistResponse;
import com.myticket.common.enums.NotificationType;
import com.myticket.common.enums.WaitlistStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository tierRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    // Setter-injected to avoid circular dependency with TicketService
    private TicketService ticketService;

    public WaitlistService(WaitlistRepository waitlistRepository, EventRepository eventRepository,
                           TicketTierRepository tierRepository, UserRepository userRepository,
                           EmailService emailService, NotificationService notificationService,
                           AuditLogService auditLogService) {
        this.waitlistRepository = waitlistRepository;
        this.eventRepository = eventRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setTicketService(@Lazy TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Transactional
    public WaitlistResponse joinWaitlist(Long userId, Long eventId, Long tierId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        TicketTier tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new RuntimeException("Tier not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify tier is actually full
        if (tier.getTicketsSold() < tier.getCapacity()) {
            throw new IllegalStateException("Tier is not full yet — book directly instead of joining waitlist");
        }

        // Check user not already on waitlist for this event+tier
        if (waitlistRepository.findByUserIdAndEventIdAndTierId(userId, eventId, tierId).isPresent()) {
            throw new IllegalStateException("Already on the waitlist for this event and tier");
        }

        Waitlist entry = Waitlist.builder()
                .user(user)
                .event(event)
                .tier(tier)
                .status(WaitlistStatus.WAITING)
                .notifyOnly(false)
                .position(0)
                .build();

        Waitlist saved = waitlistRepository.save(entry);

        emailService.sendWaitlistSlotAvailable(user.getEmail(), user.getFullName(),
                event.getTitle(), "You are on the waitlist for " + event.getTitle());

        auditLogService.logAction(user.getEmail(), "WAITLIST_JOINED", "Joined waitlist for event " + eventId);

        return mapToResponse(saved);
    }

    @Transactional
    public void notifyNextInLine(Long eventId, Long tierId) {
        // Find the earliest WAITING entry (not notifyOnly) for this event+tier
        waitlistRepository.findFirstByEventIdAndTierIdAndStatusAndNotifyOnlyFalseOrderByJoinedAtAsc(
                eventId, tierId, WaitlistStatus.WAITING
        ).ifPresent(entry -> {
            String claimToken = UUID.randomUUID().toString();
            entry.setClaimToken(claimToken);
            entry.setClaimExpiresAt(LocalDateTime.now().plusHours(24));
            entry.setStatus(WaitlistStatus.NOTIFIED);
            waitlistRepository.save(entry);

            String claimLink = "/api/waitlist/claim?token=" + claimToken;
            emailService.sendWaitlistSlotAvailable(
                    entry.getUser().getEmail(),
                    entry.getUser().getFullName(),
                    entry.getEvent().getTitle(),
                    claimLink
            );

            notificationService.createNotification(
                    entry.getUser().getId(),
                    NotificationType.WAITLIST_SLOT,
                    "A slot is available for " + entry.getEvent().getTitle() + "! Claim it within 24 hours.",
                    eventId
            );
        });

        // Also notify notifyOnly users (they don't claim, just get informed)
        List<Waitlist> notifyOnlyEntries = waitlistRepository
                .findByEventIdAndStatusOrderByJoinedAtAsc(eventId, WaitlistStatus.WAITING)
                .stream()
                .filter(Waitlist::isNotifyOnly)
                .collect(Collectors.toList());

        for (Waitlist no : notifyOnlyEntries) {
            notificationService.createNotification(
                    no.getUser().getId(),
                    NotificationType.WAITLIST_SLOT,
                    "A slot may be available for " + no.getEvent().getTitle() + ". Check the event page.",
                    eventId
            );
        }
    }

    @Transactional
    public WaitlistResponse claimWaitlistSlot(String claimToken) {
        Waitlist entry = waitlistRepository.findByClaimTokenAndStatus(claimToken, WaitlistStatus.NOTIFIED)
                .orElseThrow(() -> new RuntimeException("Invalid or already used claim token"));

        if (entry.getClaimExpiresAt().isBefore(LocalDateTime.now())) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
            auditLogService.logAction(entry.getUser().getEmail(), "WAITLIST_EXPIRED", "Waitlist claim expired for event " + entry.getEvent().getId());
            throw new com.myticket.common.exception.ClaimExpiredException("Claim token has expired");
        }

        // Book a free ticket for this user+tier
        BookingRequest req = BookingRequest.builder()
                .tierId(entry.getTier().getId())
                .quantity(1)
                .build();
        ticketService.bookTicket(req, entry.getUser().getId());

        entry.setStatus(WaitlistStatus.CLAIMED);
        waitlistRepository.save(entry);

        auditLogService.logAction(entry.getUser().getEmail(), "WAITLIST_CLAIMED",
                "Claimed waitlist slot for event " + entry.getEvent().getId() + ", tier " + entry.getTier().getId());

        return mapToResponse(entry);
    }

    @Transactional
    public WaitlistResponse enableNotifyMe(Long userId, Long eventId, Long tierId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        TicketTier tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new RuntimeException("Tier not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (waitlistRepository.findByUserIdAndEventIdAndTierId(userId, eventId, tierId).isPresent()) {
            throw new IllegalStateException("Already on the waitlist for this event and tier");
        }

        Waitlist entry = Waitlist.builder()
                .user(user)
                .event(event)
                .tier(tier)
                .status(WaitlistStatus.WAITING)
                .notifyOnly(true)
                .position(0)
                .build();

        return mapToResponse(waitlistRepository.save(entry));
    }

    public List<WaitlistResponse> getUserWaitlistEntries(Long userId) {
        return waitlistRepository.findByUserIdOrderByJoinedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void leaveWaitlist(Long waitlistId, Long userId) {
        Waitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new RuntimeException("Waitlist entry not found"));
        if (!entry.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to remove this waitlist entry");
        }
        waitlistRepository.delete(entry);
    }

    private WaitlistResponse mapToResponse(Waitlist w) {
        return WaitlistResponse.builder()
                .id(w.getId())
                .eventId(w.getEvent().getId())
                .eventTitle(w.getEvent().getTitle())
                .tierId(w.getTier().getId())
                .tierName(w.getTier().getName())
                .position(w.getPosition())
                .notifyOnly(w.isNotifyOnly())
                .status(w.getStatus())
                .claimToken(w.getClaimToken())
                .claimExpiresAt(w.getClaimExpiresAt())
                .joinedAt(w.getJoinedAt())
                .build();
    }
}
