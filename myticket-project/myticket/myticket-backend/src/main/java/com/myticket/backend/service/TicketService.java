package com.myticket.backend.service;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.Ticket;
import com.myticket.backend.model.TicketTier;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.TicketRepository;
import com.myticket.backend.repository.TicketTierRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.common.dto.BookingRequest;
import com.myticket.common.dto.PaymentResponse;
import com.myticket.common.dto.TicketResponse;
import com.myticket.common.enums.TicketStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository tierRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final EventService eventService;

    public TicketService(TicketRepository ticketRepository, EventRepository eventRepository,
                         TicketTierRepository tierRepository, UserRepository userRepository,
                         QrCodeService qrCodeService, PaymentService paymentService,
                         EmailService emailService, AuditLogService auditLogService,
                         EventService eventService) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.eventService = eventService;
    }

    @Transactional
    public Object bookTicket(BookingRequest req, Long userId) {
        TicketTier tier = tierRepository.findByIdWithPessimisticWrite(req.getTierId())
                .orElseThrow(() -> new IllegalArgumentException("Tier not found"));
        Event event = eventRepository.findByIdWithPessimisticWrite(tier.getEvent().getId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (tier.getTicketsSold() + req.getQuantity() > tier.getCapacity()) {
            throw new IllegalStateException("Not enough tickets left in this tier");
        }

        if (!eventService.checkAgeEligibility(userId, event.getId())) {
            throw new IllegalStateException("User does not meet age requirements (Must be 18+)");
        }

        if (tier.isEarlyBird() && tier.isExpired()) {
            throw new IllegalStateException("This early bird tier is expired");
        }

        boolean isPaid = tier.getPrice() > 0;

        if (isPaid) {
            PaymentResponse paymentResponse = paymentService.initiatePayment(event.getId(), tier.getId(), req.getQuantity(), user.getEmail());
            String txnId = paymentResponse.getTxnId();
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
            
            for (int i = 0; i < req.getQuantity(); i++) {
                String attendeeName = user.getFullName();
                if (req.getGroupMemberNames() != null && i < req.getGroupMemberNames().size()) {
                    attendeeName = req.getGroupMemberNames().get(i);
                }
                Ticket t = Ticket.builder()
                        .user(user)
                        .tier(tier)
                        .event(event)
                        .status(TicketStatus.PENDING)
                        .paymentTxnId(txnId)
                        .reservationExpiresAt(expiry)
                        .attendeeName(attendeeName)
                        .build();
                ticketRepository.save(t);
            }
            return paymentResponse;
        } else {
            List<TicketResponse> responses = new ArrayList<>();
            String groupBookingId = req.getQuantity() > 1 ? UUID.randomUUID().toString() : null;

            for (int i = 0; i < req.getQuantity(); i++) {
                String ticketCode = UUID.randomUUID().toString();
                String qrPath = qrCodeService.generateQr(ticketCode);
                String attendeeName = user.getFullName();
                if (req.getGroupMemberNames() != null && i < req.getGroupMemberNames().size()) {
                    attendeeName = req.getGroupMemberNames().get(i);
                }

                Ticket t = Ticket.builder()
                        .user(user)
                        .tier(tier)
                        .event(event)
                        .ticketCode(ticketCode)
                        .qrImagePath(qrPath)
                        .status(TicketStatus.BOOKED)
                        .groupBookingId(groupBookingId)
                        .attendeeName(attendeeName)
                        .build();
                ticketRepository.save(t);
                responses.add(mapToResponse(t));
                emailService.sendBookingConfirmation(user.getEmail(), attendeeName, event.getTitle(), ticketCode, qrPath);
            }

            tier.setTicketsSold(tier.getTicketsSold() + req.getQuantity());
            event.setTicketsSold(event.getTicketsSold() + req.getQuantity());
            tierRepository.save(tier);
            eventRepository.save(event);

            auditLogService.logAction(user.getEmail(), "TICKET_BOOKED", "Booked " + req.getQuantity() + " free tickets for event " + event.getId());
            return responses;
        }
    }

    @Transactional
    public void confirmBooking(String txnId) {
        List<Ticket> pendingTickets = ticketRepository.findByPaymentTxnId(txnId);
        if (pendingTickets.isEmpty()) return;

        Ticket firstTicket = pendingTickets.get(0);
        if (firstTicket.getStatus() != TicketStatus.PENDING) return;

        Event event = eventRepository.findByIdWithPessimisticWrite(firstTicket.getEvent().getId()).orElseThrow();
        TicketTier tier = tierRepository.findByIdWithPessimisticWrite(firstTicket.getTier().getId()).orElseThrow();
        User user = firstTicket.getUser();

        int quantity = pendingTickets.size();
        String groupBookingId = quantity > 1 ? UUID.randomUUID().toString() : null;

        for (Ticket t : pendingTickets) {
            String ticketCode = UUID.randomUUID().toString();
            String qrPath = qrCodeService.generateQr(ticketCode);

            t.setStatus(TicketStatus.BOOKED);
            t.setReservationExpiresAt(null);
            t.setTicketCode(ticketCode);
            t.setQrImagePath(qrPath);
            t.setGroupBookingId(groupBookingId);
            
            ticketRepository.save(t);
            emailService.sendBookingConfirmation(user.getEmail(), t.getAttendeeName(), event.getTitle(), ticketCode, qrPath);
        }

        tier.setTicketsSold(tier.getTicketsSold() + quantity);
        event.setTicketsSold(event.getTicketsSold() + quantity);
        tierRepository.save(tier);
        eventRepository.save(event);

        auditLogService.logAction(user.getEmail(), "TICKET_BOOKED", "Paid and confirmed " + quantity + " tickets for event " + event.getId());
    }

    @Transactional
    public void cancelPendingTickets(String txnId) {
        List<Ticket> pendingTickets = ticketRepository.findByPaymentTxnId(txnId);
        for (Ticket t : pendingTickets) {
            if (t.getStatus() == TicketStatus.PENDING) {
                t.setStatus(TicketStatus.CANCELLED);
                t.setReservationExpiresAt(null);
                ticketRepository.save(t);
            }
        }
    }

    @Transactional
    public void transferTicket(Long ticketId, String recipientEmail, Long actorId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        if (!ticket.getUser().getId().equals(actorId) || ticket.getStatus() != TicketStatus.BOOKED) {
            throw new IllegalStateException("Not eligible for transfer");
        }

        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new IllegalArgumentException("Recipient is not registered"));
        if (!recipient.isVerified()) {
            throw new IllegalStateException("Recipient must verify their email first");
        }

        String newCode = UUID.randomUUID().toString();
        String newQr = qrCodeService.generateQr(newCode);

        ticket.setStatus(TicketStatus.TRANSFERRED);
        ticket.setTransferredTo(recipient);
        ticketRepository.save(ticket);

        Ticket newTicket = Ticket.builder()
                .user(recipient)
                .tier(ticket.getTier())
                .event(ticket.getEvent())
                .ticketCode(newCode)
                .qrImagePath(newQr)
                .status(TicketStatus.BOOKED)
                .attendeeName(recipient.getFullName())
                .build();
        ticketRepository.save(newTicket);

        emailService.sendTransferNotice(recipient.getEmail(), recipient.getFullName(), ticket.getEvent().getTitle());
        auditLogService.logAction(ticket.getUser().getEmail(), "TICKET_TRANSFERRED", "Transferred ticket " + ticketId + " to " + recipientEmail);
    }

    @Transactional
    public void cancelTicket(Long ticketId, Long actorId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        if (!ticket.getUser().getId().equals(actorId)) {
            throw new IllegalStateException("Unauthorized to cancel this ticket");
        }
        
        Event event = ticket.getEvent();
        TicketTier tier = ticket.getTier();

        if (event.getEventDate().minusHours(24).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Return window has closed");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);

        tier.setTicketsSold(tier.getTicketsSold() - 1);
        event.setTicketsSold(event.getTicketsSold() - 1);
        tierRepository.save(tier);
        eventRepository.save(event);

        emailService.sendCancellationNotice(ticket.getUser().getEmail(), ticket.getUser().getFullName(), event.getTitle());

        // TODO Phase 6 — waitlistService.notifyNextInLine(eventId, tierId)

        auditLogService.logAction(ticket.getUser().getEmail(), "TICKET_CANCELLED", "Cancelled ticket " + ticketId);
    }

    public TicketResponse mapToResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .eventId(t.getEvent().getId())
                .eventTitle(t.getEvent().getTitle())
                .tierName(t.getTier().getName())
                .attendeeName(t.getAttendeeName())
                .ticketCode(t.getTicketCode())
                .qrImagePath(t.getQrImagePath())
                .status(t.getStatus())
                .groupBookingId(t.getGroupBookingId())
                .bookedAt(t.getBookedAt())
                .usedAt(t.getUsedAt())
                .build();
    }
}
