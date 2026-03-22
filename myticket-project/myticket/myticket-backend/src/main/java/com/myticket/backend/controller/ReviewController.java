package com.myticket.backend.controller;

import com.myticket.backend.model.User;
import com.myticket.backend.repository.UserRepository;
import com.myticket.backend.service.ReviewService;
import com.myticket.common.dto.ReviewRequest;
import com.myticket.common.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/{eventId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long eventId) {
        return ResponseEntity.ok(reviewService.getReviews(eventId));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(reviewService.getAverageRating(eventId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> submitReview(@PathVariable Long eventId,
                                                        @Valid @RequestBody ReviewRequest req,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(reviewService.submitReview(eventId, user.getId(), req));
    }
}
