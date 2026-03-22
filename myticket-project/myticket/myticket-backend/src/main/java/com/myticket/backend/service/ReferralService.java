package com.myticket.backend.service;

import com.myticket.backend.model.User;
import com.myticket.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ReferralService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public ReferralService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public void applyReferral(String referralCode, Long newUserId) {
        if (referralCode == null || referralCode.isEmpty()) {
            return;
        }

        Optional<User> optionalReferrer = userRepository.findByReferralCode(referralCode);
        if (optionalReferrer.isPresent()) {
            User referrer = optionalReferrer.get();
            User newUser = userRepository.findById(newUserId).orElseThrow();

            if (newUser.getReferredBy() == null) {
                // Award points
                referrer.setReferralCredits(referrer.getReferralCredits() + 50);
                userRepository.save(referrer);

                // Mark referred
                newUser.setReferredBy(referrer.getId());
                userRepository.save(newUser);

                // Notify referrer
                emailService.sendReferralReward(
                        referrer.getEmail(),
                        referrer.getFullName(),
                        50
                );
            }
        }
    }

    public Map<String, Object> getReferralStats(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        long successfulReferrals = userRepository.findByReferredBy(userId).size();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("referralCode", user.getReferralCode());
        stats.put("successfulReferrals", successfulReferrals);
        stats.put("creditsEarned", successfulReferrals * 50);
        return stats;
    }
}
