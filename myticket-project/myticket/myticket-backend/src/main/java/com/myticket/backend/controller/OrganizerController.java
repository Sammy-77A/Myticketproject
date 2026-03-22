package com.myticket.backend.controller;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.backend.service.EventService;
import com.myticket.common.dto.EventResponse;
import com.myticket.common.dto.OrganizerProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizers")
public class OrganizerController {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;

    public OrganizerController(UserRepository userRepository, EventRepository eventRepository, EventService eventService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizerProfileResponse> getOrganizer(@PathVariable Long id) {
        User organizer = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Organizer not found"));
        
        long followerCount = userRepository.countFollowers(id);
        
        List<Event> events = eventRepository.findByOrganizerIdOrderByEventDateDesc(id);
        List<EventResponse> upcomingEvents = events.stream()
                .filter(e -> e.getStatus() == com.myticket.common.enums.EventStatus.UPCOMING)
                .map(e -> eventService.getEvent(e.getId()))
                .collect(Collectors.toList());

        OrganizerProfileResponse response = OrganizerProfileResponse.builder()
                .name(organizer.getFullName())
                .bio(organizer.getBio())
                .followerCount(followerCount)
                .upcomingEvents(upcomingEvents)
                .build();
                
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> followOrganizer(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User follower = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User organizer = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Organizer not found"));
        
        follower.getFollowing().add(organizer);
        userRepository.save(follower);
        return ResponseEntity.ok(Map.of("message", "Successfully followed " + organizer.getFullName()));
    }

    @PostMapping("/{id}/unfollow")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> unfollowOrganizer(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User follower = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User organizer = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Organizer not found"));
        
        follower.getFollowing().remove(organizer);
        userRepository.save(follower);
        return ResponseEntity.ok(Map.of("message", "Successfully unfollowed " + organizer.getFullName()));
    }

    @GetMapping("/{id}/followers/count")
    public ResponseEntity<?> getFollowersCount(@PathVariable Long id) {
        long count = userRepository.countFollowers(id);
        return ResponseEntity.ok(Map.of("followerCount", count));
    }
}
