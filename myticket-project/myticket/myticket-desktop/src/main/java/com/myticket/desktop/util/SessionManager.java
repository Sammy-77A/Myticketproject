package com.myticket.desktop.util;

import com.myticket.common.enums.Role;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Singleton managing the current authenticated session.
 * Persists the JWT to ~/.myticket.session for auto-login on restart.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    private static final Path SESSION_FILE = Path.of(System.getProperty("user.home"), ".myticket.session");

    private String jwtToken;
    private Long userId;
    private Role role;
    private String userEmail;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    // ── getters / setters ──

    public String getJwtToken() { return jwtToken; }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
        persistToken();
    }

    public Long getUserId()      { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Role getRole()        { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    // ── convenience ──

    public boolean isLoggedIn() { return jwtToken != null; }

    public boolean hasRole(Role r) { return role != null && role == r; }

    public void clear() {
        jwtToken  = null;
        userId    = null;
        role      = null;
        userEmail = null;
        deletePersistedToken();
    }

    // ── token persistence ──

    private void persistToken() {
        try {
            if (jwtToken != null) {
                Files.writeString(SESSION_FILE, jwtToken);
            }
        } catch (IOException e) {
            System.err.println("Could not persist session token: " + e.getMessage());
        }
    }

    private void deletePersistedToken() {
        try {
            Files.deleteIfExists(SESSION_FILE);
        } catch (IOException e) {
            System.err.println("Could not delete session file: " + e.getMessage());
        }
    }

    /**
     * Attempts to load a previously-persisted JWT from disk.
     * @return the token string, or null if no file exists.
     */
    public String loadPersistedToken() {
        try {
            if (Files.exists(SESSION_FILE)) {
                String token = Files.readString(SESSION_FILE).trim();
                return token.isEmpty() ? null : token;
            }
        } catch (IOException e) {
            System.err.println("Could not read session file: " + e.getMessage());
        }
        return null;
    }
}
