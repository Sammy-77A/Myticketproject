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

    List<TicketTier> findByIsEarlyBirdTrueAndIsExpiredFalseAndClosesAtBefore(LocalDateTime now);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TicketTier t WHERE t.id = :id")
    java.util.Optional<TicketTier> findByIdWithPessimisticWrite(@org.springframework.data.repository.query.Param("id") Long id);
}
