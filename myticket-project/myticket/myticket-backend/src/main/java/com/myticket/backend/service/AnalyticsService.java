package com.myticket.backend.service;

import com.myticket.backend.model.*;
import com.myticket.backend.repository.*;
import com.myticket.common.dto.analytics.*;
import com.myticket.common.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final WaitlistRepository waitlistRepository;
    private final ReviewRepository reviewRepository;
    private final ReactionRepository reactionRepository;
    private final TicketTierRepository ticketTierRepository;

    public AnalyticsService(UserRepository userRepository, EventRepository eventRepository,
                            TicketRepository ticketRepository, WaitlistRepository waitlistRepository,
                            ReviewRepository reviewRepository, ReactionRepository reactionRepository,
                            TicketTierRepository ticketTierRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.waitlistRepository = waitlistRepository;
        this.reviewRepository = reviewRepository;
        this.reactionRepository = reactionRepository;
        this.ticketTierRepository = ticketTierRepository;
    }

    @Transactional(readOnly = true)
    public SystemOverviewResponse getSystemOverview() {
        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();
        
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Ticket> monthTickets = ticketRepository.findByBookedAtBetweenAndStatusIn(
                startOfMonth, endOfMonth, List.of(TicketStatus.BOOKED, TicketStatus.USED)
        );

        long totalTicketsThisMonth = monthTickets.size();
        double totalRevenueThisMonth = monthTickets.stream()
                .mapToDouble(t -> t.getTier().getPrice())
                .sum();

        long activeWaitlists = waitlistRepository.countByStatus(WaitlistStatus.WAITING);
        long upcomingEvents = eventRepository.countByStatus(EventStatus.UPCOMING);

        return SystemOverviewResponse.builder()
                .totalUsers(totalUsers)
                .totalEvents(totalEvents)
                .totalTicketsIssuedThisMonth(totalTicketsThisMonth)
                .totalRevenueThisMonthKes(totalRevenueThisMonth)
                .activeWaitlists(activeWaitlists)
                .upcomingEvents(upcomingEvents)
                .build();
    }

    @Transactional(readOnly = true)
    public EventAnalyticsResponse getEventAnalytics(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        
        Map<LocalDate, Integer> bookingCounts = new TreeMap<>();
        Map<String, EventAnalyticsResponse.TierBreakdown> tiers = new HashMap<>();
        
        int used = 0, booked = 0, cancelled = 0, transferred = 0;
        double totalRevenue = 0;

        List<TicketTier> eventTiers = ticketTierRepository.findByEventId(eventId);

        for (TicketTier tier : eventTiers) {
            tiers.put(tier.getName(), new EventAnalyticsResponse.TierBreakdown(
                    tier.getName(), 0, 0, 0, 0.0
            ));
        }

        for (Ticket t : tickets) {
            String tierName = t.getTier().getName();
            EventAnalyticsResponse.TierBreakdown tb = tiers.computeIfAbsent(tierName, 
                    k -> new EventAnalyticsResponse.TierBreakdown(k, 0, 0, 0, 0.0));

            if (t.getStatus() == TicketStatus.BOOKED || t.getStatus() == TicketStatus.USED) {
                if (t.getStatus() == TicketStatus.USED) used++;
                if (t.getStatus() == TicketStatus.BOOKED) booked++;
                
                tb.setBooked(tb.getBooked() + 1);
                totalRevenue += t.getTier().getPrice();

                LocalDate date = t.getBookedAt().toLocalDate();
                bookingCounts.put(date, bookingCounts.getOrDefault(date, 0) + 1);
            } else if (t.getStatus() == TicketStatus.CANCELLED) {
                cancelled++;
                tb.setCancelled(tb.getCancelled() + 1);
            } else if (t.getStatus() == TicketStatus.TRANSFERRED) {
                transferred++;
                tb.setTransferred(tb.getTransferred() + 1);
            }
        }

        for (TicketTier tier : eventTiers) {
            EventAnalyticsResponse.TierBreakdown tb = tiers.get(tier.getName());
            if (tier.getCapacity() > 0) {
                tb.setPercentOfCapacity((tb.getBooked() * 100.0) / tier.getCapacity());
            }
        }

        int noShow = (event.getEventDate().isBefore(LocalDateTime.now())) ? booked : 0;
        
        EventAnalyticsResponse.AttendanceRate rate = new EventAnalyticsResponse.AttendanceRate(
                used, booked, cancelled, noShow
        );

        List<EventAnalyticsResponse.BookingDateCount> overTime = bookingCounts.entrySet().stream()
                .map(e -> new EventAnalyticsResponse.BookingDateCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        Map<String, Long> reactions = new HashMap<>();
        for (ReactionType rt : ReactionType.values()) {
            reactions.put(rt.name(), reactionRepository.countByEventIdAndType(eventId, rt));
        }

        Double avgRat = reviewRepository.findAverageRatingByEventId(eventId);
        double averageRating = (avgRat != null) ? avgRat : 0.0;

        return EventAnalyticsResponse.builder()
                .bookingsOverTime(overTime)
                .tierBreakdown(new ArrayList<>(tiers.values()))
                .attendanceRate(rate)
                .totalRevenue(totalRevenue)
                .reactionCounts(reactions)
                .averageRating(averageRating)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceReportResponse> getAttendanceReport(Long organizerId) {
        List<Event> events = eventRepository.findByOrganizerIdOrderByEventDateDesc(organizerId);
        List<AttendanceReportResponse> reports = new ArrayList<>();

        for (Event e : events) {
            long used = ticketRepository.countByEventIdAndStatus(e.getId(), TicketStatus.USED);
            long booked = ticketRepository.countByEventIdAndStatus(e.getId(), TicketStatus.BOOKED);
            long totalBooked = used + booked;
            
            double rate = (totalBooked > 0) ? (used * 100.0 / totalBooked) : 0.0;
            
            reports.add(new AttendanceReportResponse(
                    e.getTitle(),
                    e.getEventDate(),
                    e.getTotalCapacity(),
                    (int) totalBooked,
                    (int) used,
                    rate
            ));
        }

        return reports;
    }
}
