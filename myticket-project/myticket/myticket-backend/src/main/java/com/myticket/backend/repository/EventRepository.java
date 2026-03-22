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

    @Query("SELECT e FROM Event e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    org.springframework.data.domain.Page<Event> searchEvents(@org.springframework.data.repository.query.Param("keyword") String keyword, Pageable pageable);

    org.springframework.data.domain.Page<Event> findByCategoryIdAndStatus(Long categoryId, EventStatus status, Pageable pageable);

    org.springframework.data.domain.Page<Event> findByStatus(EventStatus status, Pageable pageable);
}
