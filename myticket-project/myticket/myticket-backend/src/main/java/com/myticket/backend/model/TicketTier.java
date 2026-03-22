package com.myticket.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_tiers")
public class TicketTier {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @NotNull
    private Event event;

    @NotBlank
    private String name;

    @Builder.Default
    private int price = 0;

    private int capacity;

    @Builder.Default
    private int ticketsSold = 0;

    /** Comma-separated perks */
    private String perks;

    @Builder.Default
    private boolean isEarlyBird = false;

    private LocalDateTime closesAt;
}
