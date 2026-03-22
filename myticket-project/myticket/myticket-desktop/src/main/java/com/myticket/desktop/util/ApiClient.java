package com.myticket.desktop.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * Singleton HTTP wrapper for all REST calls to the MyTicket backend.
 * Uses Java 11 HttpClient.  All calls should be invoked from a background
 * thread (JavaFX Task) — never from the JavaFX Application Thread.
 */
public class ApiClient {

    private static final ApiClient INSTANCE = new ApiClient();

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;

    private ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.baseUrl = loadBaseUrl();
    }

    public static ApiClient getInstance() {
        return INSTANCE;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // ── authenticated requests ──

    public <T> T get(String endpoint, Class<T> responseType) throws Exception {
        HttpRequest request = authorizedRequest(endpoint)
                .GET()
                .build();
        return execute(request, responseType);
    }

    public <T> T get(String endpoint, TypeReference<T> typeRef) throws Exception {
        HttpRequest request = authorizedRequest(endpoint)
                .GET()
                .build();
        return executeTypeRef(request, typeRef);
    }

    public <T> T post(String endpoint, Object body, Class<T> responseType) throws Exception {
        HttpRequest request = authorizedRequest(endpoint)
                .POST(bodyPublisher(body))
                .header("Content-Type", "application/json")
                .build();
        return execute(request, responseType);
    }

    public <T> T put(String endpoint, Object body, Class<T> responseType) throws Exception {
        HttpRequest request = authorizedRequest(endpoint)
                .PUT(bodyPublisher(body))
                .header("Content-Type", "application/json")
                .build();
        return execute(request, responseType);
    }

    public void delete(String endpoint) throws Exception {
        HttpRequest request = authorizedRequest(endpoint)
                .DELETE()
                .build();
        executeVoid(request);
    }

    // ── public (unauthenticated) requests ──

    public <T> T postPublic(String endpoint, Object body, Class<T> responseType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .POST(bodyPublisher(body))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .build();
        return execute(request, responseType);
    }

    public <T> T getPublic(String endpoint, Class<T> responseType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
        return execute(request, responseType);
    }

    // ── health check (returns true if server is reachable) ──

    public boolean isServerReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether a persisted JWT token is still valid by calling /api/health with it.
     */
    public boolean isTokenValid(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/health"))
                    .GET()
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ── internals ──

    private HttpRequest.Builder authorizedRequest(String endpoint) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .timeout(Duration.ofSeconds(15));

        String token = SessionManager.getInstance().getJwtToken();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private HttpRequest.BodyPublisher bodyPublisher(Object body) throws Exception {
        if (body == null) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
    }

    private <T> T execute(HttpRequest request, Class<T> responseType) throws Exception {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response, responseType);
        } catch (ConnectException e) {
            showConnectionError();
            throw e;
        }
    }

    private <T> T executeTypeRef(HttpRequest request, TypeReference<T> typeRef) throws Exception {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            handleStatusCodes(status, response.body());
            if (responseIsEmpty(response.body())) return null;
            return mapper.readValue(response.body(), typeRef);
        } catch (ConnectException e) {
            showConnectionError();
            throw e;
        }
    }

    private void executeVoid(HttpRequest request) throws Exception {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            handleStatusCodes(status, response.body());
        } catch (ConnectException e) {
            showConnectionError();
            throw e;
        }
    }

    private <T> T handleResponse(HttpResponse<String> response, Class<T> responseType) throws Exception {
        int status = response.statusCode();
        handleStatusCodes(status, response.body());

        if (responseType == Void.class || responseType == void.class) return null;
        if (responseIsEmpty(response.body())) return null;
        return mapper.readValue(response.body(), responseType);
    }

    private void handleStatusCodes(int status, String body) throws ApiException {
        if (status == 401) {
            SessionManager.getInstance().clear();
            Platform.runLater(() -> SceneManager.getInstance().navigateTo("/fxml/login.fxml"));
            throw new ApiException("Session expired. Please log in again.");
        }
        if (status == 403) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Access Denied");
                alert.setHeaderText(null);
                alert.setContentText("You do not have permission to perform this action.");
                alert.showAndWait();
            });
            throw new ApiException("Access denied.");
        }
        if (status >= 400) {
            String errorMsg = extractError(body);
            throw new ApiException(errorMsg != null ? errorMsg : "Request failed with status " + status);
        }
    }

    private boolean responseIsEmpty(String body) {
        return body == null || body.isBlank();
    }

    private String extractError(String body) {
        try {
            Map<String, String> map = mapper.readValue(body, new TypeReference<Map<String, String>>() {});
            return map.getOrDefault("error", body);
        } catch (Exception e) {
            return body;
        }
    }

    private void showConnectionError() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText(null);
            alert.setContentText("Cannot reach MyTicket server. Make sure the backend is running on port 8080.");
            alert.showAndWait();
        });
    }

    private String loadBaseUrl() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("desktop.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("app.api.base-url", "http://localhost:8080");
            }
        } catch (IOException e) {
            System.err.println("Could not load desktop.properties: " + e.getMessage());
        }
        return "http://localhost:8080";
    }

    // ── custom exception ──

    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
    }
}
