package com.myticket.backend.model;

import com.myticket.common.enums.TicketStatus;
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
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @NotNull
    private User user;

    @ManyToOne
    @JoinColumn(name = "tier_id")
    @NotNull
    private TicketTier tier;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @NotNull
    private Event event;

    /** For group bookings — the attendee's name */
    private String attendeeName;

    @Column(unique = true)
    private String ticketCode;

    private String qrImagePath;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TicketStatus status;

    private String groupBookingId;

    @ManyToOne
    @JoinColumn(name = "transferred_to_id")
    private User transferredTo;

    private String paymentTxnId;

    /** Set during PENDING paid bookings, cleared on confirmation */
    private LocalDateTime reservationExpiresAt;

    @CreationTimestamp
    private LocalDateTime bookedAt;

    private LocalDateTime usedAt;
}
