package com.myticket.common.dto;

import com.myticket.common.enums.EventStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String venue;

    @NotNull
    private LocalDateTime eventDate;

    @Min(1)
    private int totalCapacity;

    private Integer minAge;

    private Long categoryId;

    private EventStatus status;

    private boolean isDraft;

    private String bannerImagePath;

    private List<TicketTierRequest> tiers;
}
