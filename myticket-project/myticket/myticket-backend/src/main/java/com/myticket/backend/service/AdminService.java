package com.myticket.backend.service;

import com.myticket.backend.model.*;
import com.myticket.backend.repository.*;
import com.myticket.common.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final WaitlistRepository waitlistRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final ReactionRepository reactionRepository;
    private final TicketTierRepository ticketTierRepository;
    private final AuditLogRepository auditLogRepository;
    private final SubscriberRepository subscriberRepository;
    private final AuditLogService auditLogService;

    public AdminService(UserRepository userRepository, EventRepository eventRepository,
                        TicketRepository ticketRepository, WaitlistRepository waitlistRepository,
                        CommentRepository commentRepository, ReviewRepository reviewRepository,
                        ReactionRepository reactionRepository, TicketTierRepository ticketTierRepository,
                        AuditLogRepository auditLogRepository, SubscriberRepository subscriberRepository,
                        AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.waitlistRepository = waitlistRepository;
        this.commentRepository = commentRepository;
        this.reviewRepository = reviewRepository;
        this.reactionRepository = reactionRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.auditLogRepository = auditLogRepository;
        this.subscriberRepository = subscriberRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User changeUserRole(Long id, Role newRole, String adminEmail) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(newRole);
        userRepository.save(user);
        auditLogService.logAction(adminEmail, "USER_ROLE_CHANGED", "Changed role of user " + user.getEmail() + " to " + newRole.name());
        return user;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findAll();
    }

    @Transactional
    public void hardDeleteEvent(Long eventId) {
        ticketRepository.deleteByEventId(eventId);
        waitlistRepository.deleteByEventId(eventId);
        commentRepository.deleteByEventId(eventId);
        reactionRepository.deleteByEventId(eventId);
        reviewRepository.deleteByEventId(eventId);
        ticketTierRepository.deleteByEventId(eventId);
        eventRepository.deleteById(eventId);
    }
}
