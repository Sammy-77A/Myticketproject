package com.myticket.backend.repository;

import com.myticket.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByReferralCode(String referralCode);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM user_follows WHERE following_id = :organizerId", nativeQuery = true)
    long countFollowers(@org.springframework.data.repository.query.Param("organizerId") Long organizerId);
}
