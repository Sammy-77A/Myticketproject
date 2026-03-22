package com.myticket.backend.repository;

import com.myticket.backend.model.Performer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformerRepository extends JpaRepository<Performer, Long> {
    List<Performer> findByEventIdOrderByDisplayOrderAsc(Long eventId);
}
