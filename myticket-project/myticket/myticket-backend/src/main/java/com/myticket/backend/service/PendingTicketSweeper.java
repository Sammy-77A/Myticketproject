package com.myticket.backend.service;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.Ticket;
import com.myticket.backend.model.TicketTier;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.TicketRepository;
import com.myticket.backend.repository.TicketTierRepository;
import com.myticket.common.enums.TicketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PendingTicketSweeper {

    private static final Logger log = LoggerFactory.getLogger(PendingTicketSweeper.class);

    private final TicketRepository ticketRepository;
    private final TicketTierRepository tierRepository;
    private final EventRepository eventRepository;
    private final WaitlistService waitlistService;

    public PendingTicketSweeper(TicketRepository ticketRepository, TicketTierRepository tierRepository,
                                EventRepository eventRepository, WaitlistService waitlistService) {
        this.ticketRepository = ticketRepository;
        this.tierRepository = tierRepository;
        this.eventRepository = eventRepository;
        this.waitlistService = waitlistService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void sweepExpiredPending() {
        List<Ticket> expired = ticketRepository.findByStatusAndReservationExpiresAtBefore(TicketStatus.PENDING, LocalDateTime.now());
        for (Ticket ticket : expired) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);

            TicketTier tier = ticket.getTier();
            Event event = ticket.getEvent();
            tier.setTicketsSold(tier.getTicketsSold() - 1);
            event.setTicketsSold(event.getTicketsSold() - 1);
            tierRepository.save(tier);
            eventRepository.save(event);

            log.info("Swept expired pending ticket {} for event {}", ticket.getId(), event.getId());

            waitlistService.notifyNextInLine(event.getId(), tier.getId());
        }
    }
}
