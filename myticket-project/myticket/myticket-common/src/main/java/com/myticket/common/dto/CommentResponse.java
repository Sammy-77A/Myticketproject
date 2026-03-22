package com.myticket.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String body;
    private String authorName;
    private Long authorId;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies;
}
