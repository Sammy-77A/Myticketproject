package com.myticket.backend.model;

import com.myticket.common.enums.EventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    @NotNull
    private User organizer;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @NotBlank
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank
    private String venue;

    @NotNull
    private LocalDateTime eventDate;

    private int totalCapacity;

    @Builder.Default
    private int ticketsSold = 0;

    private Integer minAge;

    @Enumerated(EnumType.STRING)
    @NotNull
    private EventStatus status;

    @Builder.Default
    private boolean isDraft = false;

    private String bannerImagePath;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
