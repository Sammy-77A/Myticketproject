package com.myticket.backend.service;

import com.myticket.backend.model.Comment;
import com.myticket.backend.model.Event;
import com.myticket.backend.model.User;
import com.myticket.backend.repository.CommentRepository;
import com.myticket.backend.repository.EventRepository;
import com.myticket.backend.repository.UserRepository;
import com.myticket.common.dto.CommentRequest;
import com.myticket.common.dto.CommentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final UserInterestService userInterestService;

    public CommentService(CommentRepository commentRepository, EventRepository eventRepository,
                          UserRepository userRepository, AuditLogService auditLogService,
                          UserInterestService userInterestService) {
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.userInterestService = userInterestService;
    }

    @Transactional
    public CommentResponse addComment(Long eventId, Long userId, CommentRequest req) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            throw new RuntimeException("Only verified users can comment");
        }

        Comment parent = null;
        if (req.getParentId() != null) {
            parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
        }

        Comment comment = Comment.builder()
                .event(event)
                .user(user)
                .body(req.getBody())
                .parent(parent)
                .build();

        commentRepository.save(comment);

        auditLogService.log(userId, user.getEmail(), "ADD_COMMENT", "Comment", comment.getId(),
                "Comment on event " + eventId);

        // Boost user interest score for this category
        if (event.getCategory() != null) {
            userInterestService.incrementScore(userId, event.getCategory().getId(), 1);
        }

        return mapToResponse(comment);
    }

    public List<CommentResponse> getComments(Long eventId) {
        List<Comment> topLevel = commentRepository.findByEventIdAndParentIsNullOrderByCreatedAtAsc(eventId);
        return topLevel.stream()
                .map(this::mapToResponseWithReplies)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long commentId, Long actorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isOwner = comment.getUser().getId().equals(actorId);
        boolean isAdmin = actor.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    private CommentResponse mapToResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .body(c.isDeleted() ? "[comment removed]" : c.getBody())
                .authorName(c.getUser().getFullName())
                .authorId(c.getUser().getId())
                .isDeleted(c.isDeleted())
                .createdAt(c.getCreatedAt())
                .replies(List.of())
                .build();
    }

    private CommentResponse mapToResponseWithReplies(Comment c) {
        List<CommentResponse> replies = c.getId() != null ?
                commentRepository.findByParentIdOrderByCreatedAtAsc(c.getId()).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList())
                : List.of();

        return CommentResponse.builder()
                .id(c.getId())
                .body(c.isDeleted() ? "[comment removed]" : c.getBody())
                .authorName(c.getUser().getFullName())
                .authorId(c.getUser().getId())
                .isDeleted(c.isDeleted())
                .createdAt(c.getCreatedAt())
                .replies(replies)
                .build();
    }
}
