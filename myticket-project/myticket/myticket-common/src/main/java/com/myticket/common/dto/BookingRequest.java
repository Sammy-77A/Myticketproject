package com.myticket.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull
    private Long tierId;

    @Min(1)
    private int quantity;

    /** Names for group bookings — one name per additional ticket */
    private List<String> groupMemberNames;
}
