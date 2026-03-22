package com.myticket.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {
    private long totalUsers;
    private long totalEvents;
    private long totalTicketsSold;
    private long upcomingEvents;
    private long completedEvents;
}
