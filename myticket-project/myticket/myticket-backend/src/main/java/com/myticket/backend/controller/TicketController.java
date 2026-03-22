package com.myticket.backend.controller;

import com.myticket.backend.model.Ticket;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.TicketRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.backend.service.TicketService;
import com.myticket.common.dto.BookingRequest;
import com.myticket.common.dto.TicketResponse;
import com.myticket.common.enums.TicketStatus;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketController(TicketService ticketService, TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    @PostMapping("/book")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> bookTicket(@Valid @RequestBody BookingRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(ticketService.bookTicket(request, user.getId()));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketResponse>> getMyTickets(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<TicketResponse> tickets = ticketRepository.findByUserIdOrderByBookedAtDesc(user.getId())
                .stream()
                .map(ticketService::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> transferTicket(@PathVariable Long id, @RequestBody Map<String, String> body, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        String recipientEmail = body.get("recipientEmail");
        ticketService.transferTicket(id, recipientEmail, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelTicket(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        ticketService.cancelTicket(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{code}/qr")
    public ResponseEntity<Resource> getQrCode(@PathVariable String code) {
        Ticket ticket = ticketRepository.findByTicketCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ticket code"));
        
        if (ticket.getQrImagePath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        File file = new File(ticket.getQrImagePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<TicketResponse> verifyTicket(@RequestBody Map<String, String> body) {
        String ticketCode = body.get("ticketCode");
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid/Not found ticket code"));
        
        if (ticket.getStatus() != TicketStatus.BOOKED) {
            throw new IllegalStateException("Ticket cannot be verified. Status is: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        
        return ResponseEntity.ok(ticketService.mapToResponse(ticket));
    }

    @GetMapping("/export/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public void exportTickets(@PathVariable Long eventId, HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets_" + eventId + ".csv\"");
        
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);

        PrintWriter writer = response.getWriter();
        writer.println("Ticket Code,Tier,Attendee Name,Status");
        
        for (Ticket t : tickets) {
            String code = t.getTicketCode() != null ? t.getTicketCode() : "";
            String tier = t.getTier().getName();
            String name = t.getAttendeeName() != null ? t.getAttendeeName().replace(",", " ") : "";
            String status = t.getStatus().name();
            writer.println(code + "," + tier + "," + name + "," + status);
        }
        writer.flush();
    }
}
