package com.myticket.backend.security;

import com.myticket.backend.model.User;
import com.myticket.backend.repository.UserRepository;
import com.myticket.common.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    
    @Value("${app.mode:offline}")
    private String appMode;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
                                            
        if ("offline".equalsIgnoreCase(appMode)) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Google login requires internet connection");
            return;
        }

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = User.builder()
                    .fullName(name)
                    .email(email)
                    .passwordHash(UUID.randomUUID().toString()) // random password for oauth users
                    .dateOfBirth(LocalDate.of(2000, 1, 1)) // default DOB, should be updated by user later
                    .role(Role.STUDENT)
                    .isVerified(true)
                    .googleId(googleId)
                    .referralCode(UUID.randomUUID().toString())
                    .build();
            userRepository.save(user);
        } else if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setVerified(true);
            userRepository.save(user);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwt = jwtUtil.generateToken(userDetails, user.getRole(), user.getId());

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8080/oauth2/redirect")
                .queryParam("token", jwt)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
