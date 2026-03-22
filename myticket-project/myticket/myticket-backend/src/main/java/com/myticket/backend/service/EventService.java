package com.myticket.backend.service;

import com.myticket.backend.model.*;
import com.myticket.backend.repository.*;
import com.myticket.common.dto.*;
import com.myticket.common.enums.EventStatus;
import com.myticket.common.enums.NotificationType;
import com.myticket.common.enums.Role;
import com.myticket.common.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;
    private final SubscriberRepository subscriberRepository;
    private final ReactionRepository reactionRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final TicketTierService ticketTierService;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepository, TicketTierRepository ticketTierRepository,
                        UserRepository userRepository, CategoryRepository categoryRepository,
                        TicketRepository ticketRepository, SubscriberRepository subscriberRepository,
                        ReactionRepository reactionRepository, EmailService emailService,
                        AuditLogService auditLogService, TicketTierService ticketTierService,
                        NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
        this.subscriberRepository = subscriberRepository;
        this.reactionRepository = reactionRepository;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.ticketTierService = ticketTierService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Event createEvent(EventRequest req, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizer not found"));

        if (organizer.getRole() != Role.ORGANIZER && organizer.getRole() != Role.ADMIN) {
            throw new RuntimeException("User must be an organizer to create an event");
        }

        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        Event event = Event.builder()
                .organizer(organizer)
                .category(category)
                .title(req.getTitle())
                .description(req.getDescription())
                .venue(req.getVenue())
                .eventDate(req.getEventDate())
                .totalCapacity(req.getTotalCapacity())
                .minAge(req.getMinAge())
                .status(req.getStatus() != null ? req.getStatus() : EventStatus.UPCOMING)
                .isDraft(req.isDraft())
                .bannerImagePath(req.getBannerImagePath())
                .build();

        Event savedEvent = eventRepository.save(event);

        if (req.getTiers() != null && !req.getTiers().isEmpty()) {
            for (TicketTierRequest tierReq : req.getTiers()) {
                TicketTier tier = TicketTier.builder()
                        .event(savedEvent)
                        .name(tierReq.getName())
                        .price(tierReq.getPrice())
                        .capacity(tierReq.getCapacity())
                        .perks(tierReq.getPerks())
                        .isEarlyBird(tierReq.isEarlyBird())
                        .closesAt(tierReq.getClosesAt())
                        .build();
                ticketTierRepository.save(tier);
            }
        }

        auditLogService.log(organizerId, "ORGANIZER", "EVENT_CREATED", "Event", savedEvent.getId(), "Event created");

        List<Subscriber> subs = subscriberRepository.findAll();
        for (Subscriber sub : subs) {
            emailService.sendNewsletterAlert(sub.getEmail(), savedEvent.getTitle(), savedEvent.getEventDate().toString(), savedEvent.getVenue());
        }

        // Notify followers of new event
        List<User> followers = userRepository.findFollowersOf(organizer.getId());
        for (User follower : followers) {
            notificationService.createNotification(follower.getId(), NotificationType.NEW_EVENT_FROM_FOLLOWED,
                    "New event from " + organizer.getFullName() + ": " + savedEvent.getTitle(), savedEvent.getId());
        }
        
        return savedEvent;
    }

    @Transactional
    public Event updateEvent(Long id, EventRequest req, Long actorId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!event.getOrganizer().getId().equals(actorId) && actor.getRole() != Role.ADMIN) {
            throw new RuntimeException("Unauthorized to update event");
        }

        boolean venueOrDateChanged = !event.getVenue().equals(req.getVenue()) || 
                                     !event.getEventDate().equals(req.getEventDate());

        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setVenue(req.getVenue());
        event.setEventDate(req.getEventDate());
        event.setMinAge(req.getMinAge());
        if (req.getStatus() != null) event.setStatus(req.getStatus());
        event.setDraft(req.isDraft());
        if (req.getBannerImagePath() != null) event.setBannerImagePath(req.getBannerImagePath());
        event.setCategory(category);
        
        // Note: For simplicity, update does not handle completely replacing tiers here, 
        // rely on specific tire update endpoints if needed.

        Event updatedEvent = eventRepository.save(event);

        if (venueOrDateChanged) {
            List<Ticket> bookedTickets = ticketRepository.findByEventIdAndStatus(id, TicketStatus.BOOKED);
            for (Ticket ticket : bookedTickets) {
                emailService.sendEventUpdate(ticket.getUser().getEmail(), ticket.getUser().getFullName(), event.getTitle(), "Venue or date has changed. Please check the latest event details.");
                notificationService.createNotification(ticket.getUser().getId(), NotificationType.VENUE_CHANGED,
                        "Event \"" + event.getTitle() + "\" has changed venue or date. Check details.", event.getId());
            }
        }

        auditLogService.log(actorId, actor.getRole().name(), "EVENT_UPDATED", "Event", updatedEvent.getId(), "Event updated");

        return updatedEvent;
    }

    @Transactional
    public void cancelEvent(Long id, Long actorId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!event.getOrganizer().getId().equals(actorId) && actor.getRole() != Role.ADMIN) {
            throw new RuntimeException("Unauthorized to cancel event");
        }

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        List<Ticket> bookedTickets = ticketRepository.findByEventIdAndStatus(id, TicketStatus.BOOKED);
        for (Ticket ticket : bookedTickets) {
            emailService.sendCancellationNotice(ticket.getUser().getEmail(), ticket.getUser().getFullName(), event.getTitle());
            notificationService.createNotification(ticket.getUser().getId(), NotificationType.EVENT_CANCELLED,
                    "Event \"" + event.getTitle() + "\" has been cancelled.", event.getId());
        }

        auditLogService.log(actorId, actor.getRole().name(), "EVENT_CANCELLED", "Event", event.getId(), "Event cancelled");
    }

    public EventResponse getEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        List<TicketTier> tiers = ticketTierRepository.findByEventId(id);
        List<TicketTierResponse> tierResponses = tiers.stream()
                .filter(tier -> !ticketTierService.isEarlyBirdExpired(tier))
                .map(tier -> TicketTierResponse.builder()
                        .id(tier.getId())
                        .name(tier.getName())
                        .price(tier.getPrice())
                        .capacity(tier.getCapacity())
                        .ticketsSold(tier.getTicketsSold())
                        .perks(tier.getPerks())
                        .isEarlyBird(tier.isEarlyBird())
                        .closesAt(tier.getClosesAt())
                        .dealScore(ticketTierService.getDealScore(tier, event))
                        .build())
                .collect(Collectors.toList());

        long reactionCounts = reactionRepository.countByEventId(id);
        int remainingCapacity = Math.max(0, event.getTotalCapacity() - event.getTicketsSold());

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .venue(event.getVenue())
                .eventDate(event.getEventDate())
                .totalCapacity(event.getTotalCapacity())
                .ticketsSold(event.getTicketsSold())
                .minAge(event.getMinAge())
                .status(event.getStatus())
                .isDraft(event.isDraft())
                .bannerImagePath(event.getBannerImagePath())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getFullName())
                .categoryId(event.getCategory() != null ? event.getCategory().getId() : null)
                .categoryName(event.getCategory() != null ? event.getCategory().getName() : null)
                .createdAt(event.getCreatedAt())
                .tiers(tierResponses)
                .reactionCounts(reactionCounts)
                .remainingCapacity(remainingCapacity)
                .shareUrl("https://myticket.app/events/" + event.getId())
                .whatsAppShareUrl("https://wa.me/?text=" + java.net.URLEncoder.encode(event.getTitle() + " - https://myticket.app/events/" + event.getId(), java.nio.charset.StandardCharsets.UTF_8))
                .twitterShareUrl("https://twitter.com/intent/tweet?text=" + java.net.URLEncoder.encode(event.getTitle(), java.nio.charset.StandardCharsets.UTF_8) + "&url=" + java.net.URLEncoder.encode("https://myticket.app/events/" + event.getId(), java.nio.charset.StandardCharsets.UTF_8))
                .facebookShareUrl("https://www.facebook.com/sharer/sharer.php?u=" + java.net.URLEncoder.encode("https://myticket.app/events/" + event.getId(), java.nio.charset.StandardCharsets.UTF_8))
                .build();
    }

    public Page<Event> listEvents(String categoryId, String status, String search, Long organizerId, Pageable pageable) {
        if (organizerId != null) {
            return eventRepository.findByOrganizerId(organizerId, pageable);
        }
        if (search != null && !search.isEmpty()) {
            return eventRepository.searchEvents(search, pageable);
        } else if (categoryId != null && !categoryId.isEmpty() && status != null && !status.isEmpty()) {
            return eventRepository.findByCategoryIdAndStatus(Long.parseLong(categoryId), EventStatus.valueOf(status), pageable);
        } else if (status != null && !status.isEmpty()) {
            return eventRepository.findByStatus(EventStatus.valueOf(status), pageable);
        } else {
            return eventRepository.findAll(pageable);
        }
    }

    public List<User> getEventAttendees(Long id, Long actorId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!event.getOrganizer().getId().equals(actorId) && actor.getRole() != Role.ADMIN) {
            throw new RuntimeException("Unauthorized to view attendees");
        }

        List<Ticket> bookedTickets = ticketRepository.findByEventIdAndStatus(id, TicketStatus.BOOKED);
        return bookedTickets.stream()
                .map(Ticket::getUser)
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean checkAgeEligibility(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getMinAge() == null) {
            return true;
        }

        int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < event.getMinAge()) {
            throw new RuntimeException("This event requires attendees to be " + event.getMinAge() + " or older");
        }

        return true;
    }
}
