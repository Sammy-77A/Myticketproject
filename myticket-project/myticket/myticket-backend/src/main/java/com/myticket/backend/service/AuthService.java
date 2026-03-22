package com.myticket.backend.service;

import com.myticket.backend.model.User;
import com.myticket.backend.repository.UserRepository;
import com.myticket.backend.security.JwtUtil;
import com.myticket.common.dto.AuthResponse;
import com.myticket.common.dto.LoginRequest;
import com.myticket.common.dto.RegisterRequest;
import com.myticket.common.enums.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;
    private final CaptchaService captchaService;
    private final ReferralService referralService;
    private final AuditLogService auditLogService;

    // In-memory token store for email verification demo
    private final Map<String, String> verificationTokens = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       EmailService emailService,
                       CaptchaService captchaService,
                       ReferralService referralService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.emailService = emailService;
        this.captchaService = captchaService;
        this.referralService = referralService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (!captchaService.verify(req.getCaptchaToken())) {
            throw new IllegalArgumentException("Invalid captcha");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String phone = req.getPhoneNumber();
        if (phone != null) {
            if (!phone.matches("^2547\\d{8}$")) {
                throw new IllegalArgumentException("Phone number must be in Safaricom format 2547XXXXXXXX");
            }
        }

        User referrer = null;
        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            referrer = userRepository.findByReferralCode(req.getReferralCode())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid referral code"));
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .dateOfBirth(req.getDateOfBirth())
                .phoneNumber(phone)
                .role(Role.STUDENT) // Default role for registration
                .isVerified(false)
                .referralCode(UUID.randomUUID().toString())
                .referredBy(referrer != null ? referrer.getId() : null)
                .build();

        userRepository.save(user);

        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            referralService.applyReferral(req.getReferralCode(), user.getId());
        }

        String verifyToken = UUID.randomUUID().toString();
        verificationTokens.put(verifyToken, user.getEmail());

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verifyToken);
        auditLogService.log(user.getId(), user.getEmail(), "USER_REGISTERED", "User", user.getId(), "User registered");
    }

    @Transactional
    public void verifyEmail(String token) {
        String email = verificationTokens.remove(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setVerified(true);
        userRepository.save(user);
        auditLogService.log(user.getId(), user.getEmail(), "EMAIL_VERIFIED", "User", user.getId(), "Email verified");
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isVerified()) {
            throw new IllegalStateException("Email not verified");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getRole(), user.getId());

        auditLogService.log(user.getId(), user.getEmail(), "USER_LOGIN", "User", user.getId(), "User logged in");

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }

    public AuthResponse refreshToken(String oldToken) {
        if (oldToken != null && oldToken.startsWith("Bearer ")) {
            oldToken = oldToken.substring(7);
        }

        if (!jwtUtil.validateToken(oldToken)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String email = jwtUtil.extractEmail(oldToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newToken = jwtUtil.generateToken(userDetails, user.getRole(), user.getId());

        return AuthResponse.builder()
                .token(newToken)
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }

    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect old password");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.logAction(user.getEmail(), "PASSWORD_CHANGED", "User changed their password");
    }
}
