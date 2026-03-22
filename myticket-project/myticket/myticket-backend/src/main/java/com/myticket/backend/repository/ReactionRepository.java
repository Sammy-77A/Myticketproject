package com.myticket.backend.repository;

import com.myticket.backend.model.Reaction;
import com.myticket.common.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    List<Reaction> findByEventIdAndType(Long eventId, ReactionType type);

    Optional<Reaction> findByEventIdAndUserId(Long eventId, Long userId);
}
