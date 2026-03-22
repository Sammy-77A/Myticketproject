package com.myticket.backend.service;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.Reaction;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.ReactionRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.common.enums.ReactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final UserInterestService userInterestService;

    public ReactionService(ReactionRepository reactionRepository, EventRepository eventRepository,
                           UserRepository userRepository, UserInterestService userInterestService) {
        this.reactionRepository = reactionRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.userInterestService = userInterestService;
    }

    @Transactional
    public Map<String, Object> toggleReaction(Long eventId, Long userId, ReactionType type) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Reaction> existing = reactionRepository.findByEventIdAndUserId(eventId, userId);

        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            if (reaction.getType() == type) {
                // Same type → unreact
                reactionRepository.delete(reaction);
            } else {
                // Different type → update
                reaction.setType(type);
                reactionRepository.save(reaction);
            }
        } else {
            // New reaction
            Reaction reaction = Reaction.builder()
                    .event(event)
                    .user(user)
                    .type(type)
                    .build();
            reactionRepository.save(reaction);

            // Boost user interest score
            if (event.getCategory() != null) {
                userInterestService.incrementScore(userId, event.getCategory().getId(), 1);
            }
        }

        return getReactionCounts(eventId);
    }

    public Map<String, Object> getReactionCounts(Long eventId) {
        long interested = reactionRepository.findByEventIdAndType(eventId, ReactionType.INTERESTED).size();
        long going = reactionRepository.findByEventIdAndType(eventId, ReactionType.GOING).size();
        return Map.of(
                "INTERESTED", interested,
                "GOING", going,
                "total", interested + going
        );
    }

    public ReactionType getUserReaction(Long eventId, Long userId) {
        return reactionRepository.findByEventIdAndUserId(eventId, userId)
                .map(Reaction::getType)
                .orElse(null);
    }
}
