package com.myticket.common.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAnalyticsResponse {
    private List<BookingDateCount> bookingsOverTime;
    private List<TierBreakdown> tierBreakdown;
    private AttendanceRate attendanceRate;
    private double totalRevenue;
    private Map<String, Long> reactionCounts;
    private double averageRating;

    @Data
    @AllArgsConstructor
    public static class BookingDateCount {
        private LocalDate date;
        private int count;
    }

    @Data
    @AllArgsConstructor
    public static class TierBreakdown {
        private String tierName;
        private int booked;
        private int cancelled;
        private int transferred;
        private double percentOfCapacity;
    }

    @Data
    @AllArgsConstructor
    public static class AttendanceRate {
        private int used;
        private int booked;
        private int cancelled;
        private int noShow;
    }
}
