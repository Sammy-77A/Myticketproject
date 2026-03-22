package com.myticket.backend.controller;

import com.myticket.backend.model.Event;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.backend.service.AnalyticsService;
import com.myticket.common.dto.analytics.AttendanceReportResponse;
import com.myticket.common.dto.analytics.EventAnalyticsResponse;
import com.myticket.common.dto.analytics.SystemOverviewResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public AnalyticsController(AnalyticsService analyticsService, EventRepository eventRepository, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemOverviewResponse> getSystemOverview() {
        return ResponseEntity.ok(analyticsService.getSystemOverview());
    }

    @GetMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventAnalyticsResponse> getEventAnalytics(@PathVariable Long id, 
                                                                    @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found"));
            if (!event.getOrganizer().getEmail().equals(userDetails.getUsername())) {
                return ResponseEntity.status(403).build();
            }
        }
        
        return ResponseEntity.ok(analyticsService.getEventAnalytics(id));
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<AttendanceReportResponse>> getMyEventsAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return ResponseEntity.ok(analyticsService.getAttendanceReport(user.getId()));
    }
}
