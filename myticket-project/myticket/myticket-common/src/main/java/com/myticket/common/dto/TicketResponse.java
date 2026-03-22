package com.myticket.common.dto;

import com.myticket.common.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String tierName;
    private String attendeeName;
    private String ticketCode;
    private String qrImagePath;
    private TicketStatus status;
    private String groupBookingId;
    private LocalDateTime bookedAt;
    private LocalDateTime usedAt;
}
