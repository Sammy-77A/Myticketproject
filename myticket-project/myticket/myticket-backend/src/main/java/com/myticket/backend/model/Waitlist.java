package com.myticket.backend.model;

import com.myticket.common.enums.WaitlistStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "waitlists")
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @NotNull
    private Event event;

    @ManyToOne
    @JoinColumn(name = "tier_id")
    @NotNull
    private TicketTier tier;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @NotNull
    private User user;

    private int position;

    /**
     * If true, user receives notification but is NOT auto-allocated a ticket slot.
     */
    @Builder.Default
    private boolean notifyOnly = false;

    private String claimToken;

    private LocalDateTime claimExpiresAt;

    @Enumerated(EnumType.STRING)
    @NotNull
    private WaitlistStatus status;

    @CreationTimestamp
    private LocalDateTime joinedAt;
}
