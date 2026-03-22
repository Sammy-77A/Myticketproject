package com.myticket.backend.controller;

import com.myticket.backend.model.User;
import com.myticket.backend.repository.UserRepository;
import com.myticket.backend.service.ReferralService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final ReferralService referralService;

    public UserController(UserRepository userRepository, ReferralService referralService) {
        this.userRepository = userRepository;
        this.referralService = referralService;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(getCurrentUser(userDetails));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> updateMe(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, String> body) {
        User user = getCurrentUser(userDetails);
        if (body.containsKey("fullName")) {
            user.setFullName(body.get("fullName"));
        }
        if (body.containsKey("bio")) {
            user.setBio(body.get("bio"));
        }
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me/referral-stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getReferralStats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(referralService.getReferralStats(user.getId()));
    }
}
