package com.myticket.backend.service;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.UserInterest;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.TicketRepository;
import com.myticket.backend.repository.UserInterestRepository;
import com.myticket.common.dto.EventResponse;
import com.myticket.common.enums.EventStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final UserInterestRepository userInterestRepository;
    private final EventService eventService;

    public RecommendationService(EventRepository eventRepository, TicketRepository ticketRepository,
                                  UserInterestRepository userInterestRepository, EventService eventService) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.userInterestRepository = userInterestRepository;
        this.eventService = eventService;
    }

    public List<EventResponse> getRecommendations(Long userId, int limit) {
        long bookingCount = ticketRepository.countByUserId(userId);

        List<EventResponse> results;

        if (bookingCount >= 3) {
            results = personalRecommendations(userId, limit);
        } else if (bookingCount >= 1) {
            results = categoryRecommendations(userId, limit);
        } else {
            results = trendingRecommendations(limit);
        }

        // Pad with trending if needed
        if (results.size() < limit) {
            List<EventResponse> trending = trendingRecommendations(limit);
            Set<Long> existingIds = results.stream().map(EventResponse::getId).collect(Collectors.toSet());
            for (EventResponse e : trending) {
                if (!existingIds.contains(e.getId()) && results.size() < limit) {
                    results.add(e);
                }
            }
        }

        return results;
    }

    public List<EventResponse> personalRecommendations(Long userId, int limit) {
        List<UserInterest> interests = userInterestRepository.findByUserIdOrderByScoreDesc(userId);
        Set<Long> bookedEventIds = ticketRepository.findBookedEventIdsByUserId(userId);

        List<Event> recommended = new ArrayList<>();
        int categoriesChecked = 0;

        for (UserInterest interest : interests) {
            if (categoriesChecked >= 3) break;
            Long categoryId = interest.getCategory().getId();

            List<Event> events = eventRepository.findByStatusAndEventDateAfter(EventStatus.UPCOMING, LocalDateTime.now())
                    .stream()
                    .filter(e -> e.getCategory() != null && e.getCategory().getId().equals(categoryId))
                    .filter(e -> !bookedEventIds.contains(e.getId()))
                    .collect(Collectors.toList());

            recommended.addAll(events);
            categoriesChecked++;
        }

        // De-duplicate and limit
        return recommended.stream()
                .distinct()
                .limit(limit)
                .map(e -> eventService.getEvent(e.getId()))
                .collect(Collectors.toList());
    }

    public List<EventResponse> categoryRecommendations(Long userId, int limit) {
        Set<Long> bookedEventIds = ticketRepository.findBookedEventIdsByUserId(userId);
        Set<Long> bookedCategoryIds = ticketRepository.findBookedCategoryIdsByUserId(userId);

        List<Event> events = eventRepository.findByStatusAndEventDateAfter(EventStatus.UPCOMING, LocalDateTime.now())
                .stream()
                .filter(e -> e.getCategory() != null && bookedCategoryIds.contains(e.getCategory().getId()))
                .filter(e -> !bookedEventIds.contains(e.getId()))
                .limit(limit)
                .collect(Collectors.toList());

        return events.stream()
                .map(e -> eventService.getEvent(e.getId()))
                .collect(Collectors.toList());
    }

    public List<EventResponse> trendingRecommendations(int limit) {
        List<Event> trending = eventRepository.findTop7UpcomingByTicketsSold(PageRequest.of(0, limit));
        return trending.stream()
                .map(e -> eventService.getEvent(e.getId()))
                .collect(Collectors.toList());
    }
}
