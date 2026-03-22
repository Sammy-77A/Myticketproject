package com.myticket.backend.controller;

import com.myticket.backend.model.AuditLog;
import com.myticket.backend.model.Subscriber;
import com.myticket.backend.model.User;
import com.myticket.backend.service.AdminService;
import com.myticket.common.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> changeUserRole(@PathVariable Long id, @RequestParam Role role, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(adminService.changeUserRole(id, role, userDetails.getUsername()));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAuditLogs(pageable));
    }

    @GetMapping("/subscribers")
    public ResponseEntity<List<Subscriber>> getAllSubscribers() {
        return ResponseEntity.ok(adminService.getAllSubscribers());
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> hardDeleteEvent(@PathVariable Long id) {
        adminService.hardDeleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
