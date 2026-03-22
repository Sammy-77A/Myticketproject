package com.myticket.backend.repository;

import com.myticket.backend.model.Waitlist;
import com.myticket.common.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    List<Waitlist> findByEventIdAndStatusOrderByJoinedAtAsc(Long eventId, WaitlistStatus status);

    Optional<Waitlist> findByUserIdAndEventId(Long userId, Long eventId);

    Optional<Waitlist> findByUserIdAndEventIdAndTierId(Long userId, Long eventId, Long tierId);

    long countByEventIdAndStatus(Long eventId, WaitlistStatus status);

    Optional<Waitlist> findFirstByEventIdAndTierIdAndStatusAndNotifyOnlyFalseOrderByJoinedAtAsc(
            Long eventId, Long tierId, WaitlistStatus status);

    Optional<Waitlist> findByClaimTokenAndStatus(String claimToken, WaitlistStatus status);

    List<Waitlist> findByUserIdOrderByJoinedAtDesc(Long userId);

    List<Waitlist> findByStatusAndClaimExpiresAtBefore(WaitlistStatus status, LocalDateTime now);

    long countByStatus(WaitlistStatus status);

    void deleteByEventId(Long eventId);
}
