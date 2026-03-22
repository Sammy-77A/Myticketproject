package com.myticket.backend.service;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.Review;
import com.myticket.backend.model.Ticket;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.ReviewRepository;
import com.myticket.backend.repository.TicketRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.common.dto.ReviewRequest;
import com.myticket.common.dto.ReviewResponse;
import com.myticket.common.enums.TicketStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public ReviewService(ReviewRepository reviewRepository, EventRepository eventRepository,
                         UserRepository userRepository, TicketRepository ticketRepository,
                         AuditLogService auditLogService) {
        this.reviewRepository = reviewRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReviewResponse submitReview(Long eventId, Long userId, ReviewRequest req) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Event must be completed (past date)
        if (event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("You can only review events that have already occurred");
        }

        // User must have a USED ticket
        List<Ticket> usedTickets = ticketRepository.findByEventIdAndUserIdAndStatus(eventId, userId, TicketStatus.USED);
        if (usedTickets.isEmpty()) {
            throw new RuntimeException("You must have attended this event (USED ticket) to leave a review");
        }

        // Check for existing review
        if (reviewRepository.findByEventIdAndUserId(eventId, userId).isPresent()) {
            throw new RuntimeException("You have already reviewed this event");
        }

        // Validate star rating
        if (req.getStarRating() < 1 || req.getStarRating() > 5) {
            throw new RuntimeException("Star rating must be between 1 and 5");
        }

        Review review = Review.builder()
                .event(event)
                .user(user)
                .starRating(req.getStarRating())
                .body(req.getBody())
                .build();

        reviewRepository.save(review);

        auditLogService.log(userId, user.getEmail(), "REVIEW_SUBMITTED", "Review", review.getId(),
                "Reviewed event " + eventId + " with " + req.getStarRating() + " stars");

        return mapToResponse(review);
    }

    public List<ReviewResponse> getReviews(Long eventId) {
        return reviewRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getUserReviews(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getAverageRating(Long eventId) {
        Double avg = reviewRepository.findAverageRatingByEventId(eventId);
        long count = reviewRepository.countByEventId(eventId);
        return Map.of(
                "average", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
                "count", count
        );
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf("@");
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private ReviewResponse mapToResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .starRating(r.getStarRating())
                .body(r.getBody())
                .authorName(maskEmail(r.getUser().getEmail()))
                .authorId(r.getUser().getId())
                .eventTitle(r.getEvent().getTitle())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
