package com.myticket.backend.repository;

import com.myticket.backend.model.Ticket;
import com.myticket.common.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUserIdOrderByBookedAtDesc(Long userId);

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByEventIdAndStatus(Long eventId, TicketStatus status);

    long countByEventIdAndStatus(Long eventId, TicketStatus status);
}
