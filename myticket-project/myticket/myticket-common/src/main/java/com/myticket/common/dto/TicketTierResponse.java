package com.myticket.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTierResponse {
    private Long id;
    private String name;
    private int price;
    private int capacity;
    private int ticketsSold;
    private String perks;
    private boolean isEarlyBird;
    private LocalDateTime closesAt;
}
