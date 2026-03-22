package com.myticket.backend.repository;

import com.myticket.backend.model.TicketTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {

    List<TicketTier> findByEventId(Long eventId);

    List<TicketTier> findByEventIdAndClosesAtAfter(Long eventId, LocalDateTime now);
}
