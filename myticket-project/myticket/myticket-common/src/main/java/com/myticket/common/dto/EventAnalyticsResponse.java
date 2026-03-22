package com.myticket.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAnalyticsResponse {
    private Long eventId;
    private String eventTitle;
    private int totalCapacity;
    private int ticketsSold;
    private int ticketsUsed;
    private int ticketsCancelled;
    private int waitlistSize;
    private int interestedCount;
    private int goingCount;
    private Map<String, Integer> tierBreakdown;
}
