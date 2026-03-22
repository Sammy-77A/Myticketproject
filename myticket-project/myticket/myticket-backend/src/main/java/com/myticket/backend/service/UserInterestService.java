package com.myticket.backend.service;

import com.myticket.backend.model.Category;
import com.myticket.backend.model.User;
import com.myticket.backend.model.UserInterest;
import com.myticket.backend.repository.CategoryRepository;
import com.myticket.backend.repository.UserInterestRepository;
import com.myticket.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserInterestService {

    private final UserInterestRepository userInterestRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public UserInterestService(UserInterestRepository userInterestRepository,
                               UserRepository userRepository,
                               CategoryRepository categoryRepository) {
        this.userInterestRepository = userInterestRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void incrementScore(Long userId, Long categoryId, int delta) {
        Optional<UserInterest> existing = userInterestRepository.findByUserIdAndCategoryId(userId, categoryId);

        if (existing.isPresent()) {
            UserInterest interest = existing.get();
            interest.setScore(interest.getScore() + delta);
            userInterestRepository.save(interest);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            UserInterest interest = UserInterest.builder()
                    .user(user)
                    .category(category)
                    .score(delta)
                    .build();
            userInterestRepository.save(interest);
        }
    }
}
