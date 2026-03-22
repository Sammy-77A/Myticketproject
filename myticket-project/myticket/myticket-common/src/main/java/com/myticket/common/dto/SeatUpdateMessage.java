package com.myticket.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatUpdateMessage {
    private Long eventId;
    private Long tierId;
    private int remainingSeats;
    private int totalCapacity;
}
