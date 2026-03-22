package com.myticket.backend.controller;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.service.EventService;
import com.myticket.backend.service.FileStorageService;
import com.myticket.common.dto.EventRequest;
import com.myticket.common.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final FileStorageService fileStorageService;

    public EventController(EventService eventService, EventRepository eventRepository, FileStorageService fileStorageService) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Event> createEvent(@Valid @RequestBody EventRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Long organizerId = extractUserId(userDetails);
        Event event = eventService.createEvent(request, organizerId);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Long actorId = extractUserId(userDetails);
        Event event = eventService.updateEvent(id, request, actorId);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> cancelEvent(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long actorId = extractUserId(userDetails);
        eventService.cancelEvent(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        EventResponse response = eventService.getEvent(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<Event>> listEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<Event> events = eventService.listEvents(category, status, search, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}/attendees")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<User>> getEventAttendees(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long actorId = extractUserId(userDetails);
        List<User> attendees = eventService.getEventAttendees(id, actorId);
        return ResponseEntity.ok(attendees);
    }

    @PostMapping("/{id}/banner")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Event> uploadBanner(@PathVariable Long id, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {
        Long actorId = extractUserId(userDetails);
        Event event = eventRepository.findById(id).orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Authorization check
        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!event.getOrganizer().getId().equals(actorId) && !isAdmin) {
            throw new RuntimeException("Unauthorized");
        }

        String path = fileStorageService.storeFile(file);
        event.setBannerImagePath(path);
        Event updated = eventRepository.save(event);
        return ResponseEntity.ok(updated);
    }

    private Long extractUserId(UserDetails userDetails) {
        // JwtUtil extracts userId as LONG claim. Here we just convert the username to Long if needed, 
        // but wait, UserDetails username is email. 
        // We probably need to load the user by email to get ID. Or assume UserDetails is a custom implementation.
        // Assuming custom User implementation or loading by email for now.
        // I will just use a helper method to be replaced with actual claim extraction if needed.
        if (userDetails instanceof com.myticket.backend.model.User) {
            return ((com.myticket.backend.model.User) userDetails).getId();
        }
        throw new RuntimeException("Could not extract user ID");
    }
}
