package com.myticket.backend.service;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.Ticket;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.TicketRepository;
import com.myticket.common.enums.EventStatus;
import com.myticket.common.enums.TicketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EventReminderJob {

    private static final Logger log = LoggerFactory.getLogger(EventReminderJob.class);

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final EmailService emailService;

    public EventReminderJob(EventRepository eventRepository, TicketRepository ticketRepository,
                            EmailService emailService) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendReminders() {
        LocalDateTime start = LocalDateTime.now().plusHours(23);
        LocalDateTime end = LocalDateTime.now().plusHours(25);

        List<Event> events = eventRepository.findByEventDateBetweenAndStatus(start, end, EventStatus.UPCOMING);
        for (Event event : events) {
            List<Ticket> bookedTickets = ticketRepository.findByEventIdAndStatus(event.getId(), TicketStatus.BOOKED);
            for (Ticket ticket : bookedTickets) {
                emailService.sendEventReminder(
                        ticket.getUser().getEmail(),
                        ticket.getUser().getFullName(),
                        event.getTitle(),
                        event.getEventDate().toString()
                );
            }
            log.info("Sent {} reminders for event {}", bookedTickets.size(), event.getTitle());
        }
    }
}
