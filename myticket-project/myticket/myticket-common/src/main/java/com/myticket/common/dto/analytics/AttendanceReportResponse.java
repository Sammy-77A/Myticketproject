package com.myticket.common.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportResponse {
    private String eventTitle;
    private LocalDateTime eventDate;
    private int capacity;
    private int booked;
    private int attended;
    private double attendanceRatePercent;
}
