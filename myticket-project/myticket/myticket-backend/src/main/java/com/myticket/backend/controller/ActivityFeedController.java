package com.myticket.backend.controller;

import com.myticket.backend.model.AuditLog;
import com.myticket.backend.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feed")
public class ActivityFeedController {

    private final AuditLogRepository auditLogRepository;

    private static final List<String> PUBLIC_ACTIONS = List.of("BOOK_TICKET", "TICKET_BOOKED", "CREATE_EVENT", "VERIFY_TICKET");

    public ActivityFeedController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLog> auditPage = auditLogRepository.findByActionIn(
                PUBLIC_ACTIONS,
                PageRequest.of(page, size, Sort.by("timestamp").descending())
        );

        List<Map<String, Object>> items = auditPage.getContent().stream()
                .map(log -> Map.<String, Object>of(
                        "actorEmail", maskEmail(log.getActorEmail()),
                        "action", log.getAction(),
                        "detail", log.getDetail() != null ? log.getDetail() : "",
                        "timestamp", log.getTimestamp().toString()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", auditPage.getNumber(),
                "totalPages", auditPage.getTotalPages(),
                "totalItems", auditPage.getTotalElements()
        ));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf("@");
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
