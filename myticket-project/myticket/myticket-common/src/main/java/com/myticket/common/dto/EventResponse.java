package com.myticket.common.dto;

import com.myticket.common.enums.EventStatus;
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
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String venue;
    private LocalDateTime eventDate;
    private int totalCapacity;
    private int ticketsSold;
    private Integer minAge;
    private EventStatus status;
    private boolean isDraft;
    private String bannerImagePath;
    private String organizerName;
    private Long organizerId;
    private String categoryName;
    private Long categoryId;
    private LocalDateTime createdAt;
    private List<TicketTierResponse> tiers;
    private long reactionCounts;
    private int remainingCapacity;
}
