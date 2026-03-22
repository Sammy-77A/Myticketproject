package com.myticket.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUpdateMessage {
    private Long eventId;
    private int checkedIn;
    private int totalCapacity;
    private String lastScannedName;
    private String lastScannedTier;
}
