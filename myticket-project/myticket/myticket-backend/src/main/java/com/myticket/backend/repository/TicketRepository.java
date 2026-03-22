package com.myticket.backend.repository;

import com.myticket.backend.model.Ticket;
import com.myticket.common.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUserIdOrderByBookedAtDesc(Long userId);

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByEventIdAndStatus(Long eventId, TicketStatus status);

    long countByEventIdAndStatus(Long eventId, TicketStatus status);

    List<Ticket> findByPaymentTxnId(String paymentTxnId);

    List<Ticket> findByStatusAndReservationExpiresAtBefore(TicketStatus status, java.time.LocalDateTime now);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.id = :id")
    java.util.Optional<Ticket> findByIdWithPessimisticWrite(@org.springframework.data.repository.query.Param("id") Long id);

    List<Ticket> findByEventId(Long eventId);
}


