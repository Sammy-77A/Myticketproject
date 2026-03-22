package com.myticket.backend.controller;

import com.myticket.backend.service.WaitlistService;
import com.myticket.backend.security.JwtUtil;
import com.myticket.common.dto.WaitlistResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final JwtUtil jwtUtil;

    public WaitlistController(WaitlistService waitlistService, JwtUtil jwtUtil) {
        this.waitlistService = waitlistService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/join")
    public ResponseEntity<WaitlistResponse> joinWaitlist(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> body) {
        Long userId = jwtUtil.extractUserId(authHeader.replace("Bearer ", ""));
        WaitlistResponse response = waitlistService.joinWaitlist(userId, body.get("eventId"), body.get("tierId"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notify-me")
    public ResponseEntity<WaitlistResponse> enableNotifyMe(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> body) {
        Long userId = jwtUtil.extractUserId(authHeader.replace("Bearer ", ""));
        WaitlistResponse response = waitlistService.enableNotifyMe(userId, body.get("eventId"), body.get("tierId"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/claim")
    public ResponseEntity<WaitlistResponse> claimSlot(@RequestParam String token) {
        WaitlistResponse response = waitlistService.claimWaitlistSlot(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<WaitlistResponse>> getMyWaitlist(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(waitlistService.getUserWaitlistEntries(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> leaveWaitlist(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader.replace("Bearer ", ""));
        waitlistService.leaveWaitlist(id, userId);
        return ResponseEntity.ok().build();
    }
}
