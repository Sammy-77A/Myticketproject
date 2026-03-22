package com.myticket.backend.controller;

import com.myticket.backend.model.User;
import com.myticket.backend.repository.UserRepository;
import com.myticket.backend.service.ReactionService;
import com.myticket.common.dto.ReactionRequest;
import com.myticket.common.enums.ReactionType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/events/{eventId}")
public class ReactionController {

    private final ReactionService reactionService;
    private final UserRepository userRepository;

    public ReactionController(ReactionService reactionService, UserRepository userRepository) {
        this.reactionService = reactionService;
        this.userRepository = userRepository;
    }

    @PostMapping("/react")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> toggleReaction(@PathVariable Long eventId,
                                                               @Valid @RequestBody ReactionRequest req,
                                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Map<String, Object> counts = reactionService.toggleReaction(eventId, user.getId(), req.getType());
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/reactions")
    public ResponseEntity<Map<String, Object>> getReactions(@PathVariable Long eventId,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new java.util.HashMap<>(reactionService.getReactionCounts(eventId));

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null) {
                ReactionType userReaction = reactionService.getUserReaction(eventId, user.getId());
                response.put("userReaction", userReaction);
            }
        }

        return ResponseEntity.ok(response);
    }
}
