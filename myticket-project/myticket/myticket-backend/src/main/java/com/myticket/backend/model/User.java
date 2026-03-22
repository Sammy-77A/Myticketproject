package com.myticket.backend.model;

import com.myticket.common.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String passwordHash;

    @NotNull
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Role role;

    @Builder.Default
    private boolean isVerified = false;

    private String googleId;

    /** Safaricom number in format 2547XXXXXXXX, required for M-Pesa STK Push */
    private String phoneNumber;

    @Column(unique = true)
    private String referralCode;

    @Builder.Default
    private int referralCredits = 0;

    /** ID of the user who referred this user */
    private Long referredBy;

    private String bio;

    @ManyToMany
    @JoinTable(
        name = "user_follows",
        joinColumns = @JoinColumn(name = "follower_id"),
        inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    @Builder.Default
    private Set<User> following = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
