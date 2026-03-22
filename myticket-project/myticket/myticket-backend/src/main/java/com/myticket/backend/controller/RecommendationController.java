package com.myticket.backend.controller;

import com.myticket.backend.model.User;
import com.myticket.backend.repository.UserRepository;
import com.myticket.backend.service.RecommendationService;
import com.myticket.common.dto.EventResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recommendationService, UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/personal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventResponse>> getPersonalRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(recommendationService.getRecommendations(user.getId(), limit));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<EventResponse>> getTrending(
            @RequestParam(defaultValue = "7") int limit) {
        return ResponseEntity.ok(recommendationService.trendingRecommendations(limit));
    }

    @GetMapping("/category")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventResponse>> getCategoryRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(recommendationService.categoryRecommendations(user.getId(), limit));
    }
}
