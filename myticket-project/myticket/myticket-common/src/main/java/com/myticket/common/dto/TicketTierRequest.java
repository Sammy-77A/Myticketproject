package com.myticket.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTierRequest {
    @NotBlank
    private String name;
    private int price;
    private int capacity;
    private String perks;
    private boolean isEarlyBird;
    private LocalDateTime closesAt;
}
