package com.myticket.backend.repository;

import com.myticket.backend.model.Event;
import com.myticket.common.enums.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatusAndEventDateAfter(EventStatus status, LocalDateTime date);

    List<Event> findByCategoryId(Long categoryId);

    List<Event> findByOrganizerIdOrderByEventDateDesc(Long organizerId);

    @Query("SELECT e FROM Event e WHERE e.status = 'UPCOMING' ORDER BY e.ticketsSold DESC")
    List<Event> findTop7UpcomingByTicketsSold(Pageable pageable);
}
