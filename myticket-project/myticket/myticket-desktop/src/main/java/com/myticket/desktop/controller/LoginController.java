package com.myticket.desktop.controller;

import com.myticket.common.dto.LoginRequest;
import com.myticket.common.enums.Role;
import com.myticket.desktop.util.ApiClient;
import com.myticket.desktop.util.SceneManager;
import com.myticket.desktop.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Controller for the login screen (login.fxml).
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button googleButton;
    @FXML private Circle statusDot;
    @FXML private Text statusText;

    private boolean offlineMode = false;

    @FXML
    public void initialize() {
        // Check if offline mode
        offlineMode = isOfflineMode();
        if (offlineMode) {
            googleButton.getStyleClass().clear();
            googleButton.getStyleClass().add("btn-google-disabled");
            googleButton.setDisable(true);
            Tooltip tip = new Tooltip("Requires internet connection");
            Tooltip.install(googleButton, tip);
        }

        // Check server connectivity in background
        checkServerStatus();
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        hideError();
        loginButton.setDisable(true);
        loginButton.setText("Signing in…");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        Task<Map> task = new Task<>() {
            @Override
            protected Map call() throws Exception {
                return ApiClient.getInstance().postPublic("/api/auth/login", loginRequest, Map.class);
            }
        };

        task.setOnSucceeded(event -> {
            Map result = task.getValue();
            if (result != null) {
                String token = (String) result.get("token");
                String roleStr = (String) result.get("role");
                Object userIdObj = result.get("userId");
                String userEmail = (String) result.get("email");

                SessionManager sm = SessionManager.getInstance();
                sm.setJwtToken(token);
                sm.setUserEmail(userEmail);
                try {
                    sm.setRole(Role.valueOf(roleStr));
                } catch (Exception e) {
                    sm.setRole(Role.STUDENT);
                }
                if (userIdObj instanceof Number) {
                    sm.setUserId(((Number) userIdObj).longValue());
                }

                // Navigate to dashboard
                navigateToDashboard();
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Login failed.";
            if (msg.contains("401") || msg.toLowerCase().contains("invalid") || msg.toLowerCase().contains("bad credentials")) {
                msg = "Invalid email or password";
            }
            showError(msg);
            loginButton.setDisable(false);
            loginButton.setText("Log In");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onGoogleLogin() {
        if (offlineMode) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Offline Mode");
            alert.setHeaderText(null);
            alert.setContentText("Google Sign-In is not available in offline mode.");
            alert.showAndWait();
            return;
        }

        // Open browser to OAuth URL
        try {
            String oauthUrl = ApiClient.getInstance().getBaseUrl() + "/oauth2/authorization/google";
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(oauthUrl));
        } catch (Exception e) {
            showError("Could not open browser for Google Sign-In.");
        }
    }

    private void navigateToDashboard() {
        // Placeholder — will be replaced in Phase 11 with role-based dashboard
        Platform.runLater(() -> {
            // For now, just show a simple confirmation
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Login Successful");
            alert.setHeaderText(null);
            alert.setContentText("Welcome, " + SessionManager.getInstance().getUserEmail() + "!\nRole: " + SessionManager.getInstance().getRole());
            alert.showAndWait();
        });
    }

    private void checkServerStatus() {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return ApiClient.getInstance().isServerReachable();
            }
        };

        task.setOnSucceeded(event -> {
            boolean reachable = task.getValue();
            Platform.runLater(() -> {
                if (reachable) {
                    statusDot.getStyleClass().clear();
                    statusDot.getStyleClass().add("status-dot-online");
                    statusText.setText("Connected — Online mode");
                } else {
                    statusDot.getStyleClass().clear();
                    statusDot.getStyleClass().add(offlineMode ? "status-dot-offline" : "status-dot-error");
                    statusText.setText(offlineMode ? "Connected — Offline mode" : "Cannot reach server — check if backend is running");
                }
            });
        });

        task.setOnFailed(event -> Platform.runLater(() -> {
            statusDot.getStyleClass().clear();
            statusDot.getStyleClass().add("status-dot-error");
            statusText.setText("Cannot reach server — check if backend is running");
        }));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private boolean isOfflineMode() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("desktop.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return "offline".equalsIgnoreCase(props.getProperty("app.mode", "online"));
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        });
    }

    private void hideError() {
        Platform.runLater(() -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        });
    }
}
