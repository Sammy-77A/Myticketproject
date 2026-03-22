package com.myticket.common.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOverviewResponse {
    private long totalUsers;
    private long totalEvents;
    private long totalTicketsIssuedThisMonth;
    private double totalRevenueThisMonthKes;
    private long activeWaitlists;
    private long upcomingEvents;
}
