package com.myticket.common.dto;

import com.myticket.common.enums.WaitlistStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long tierId;
    private String tierName;
    private int position;
    private boolean notifyOnly;
    private WaitlistStatus status;
    private String claimToken;
    private LocalDateTime claimExpiresAt;
    private LocalDateTime joinedAt;
}
