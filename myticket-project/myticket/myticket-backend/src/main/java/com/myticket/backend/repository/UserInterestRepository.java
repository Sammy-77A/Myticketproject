package com.myticket.backend.repository;

import com.myticket.backend.model.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByUserIdOrderByScoreDesc(Long userId);

    Optional<UserInterest> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
