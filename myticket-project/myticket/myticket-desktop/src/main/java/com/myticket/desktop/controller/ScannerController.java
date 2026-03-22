package com.myticket.desktop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myticket.desktop.network.StompWebSocketClient;
import com.myticket.desktop.util.ApiClient;
import com.myticket.desktop.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.*;

/**
 * Scanner screen controller — door-check-in interface.
 * Connects to WebSocket for live attendance counter.
 */
public class ScannerController {

    @FXML private ChoiceBox<String> eventSelector;
    @FXML private Label counterLabel;
    @FXML private TextField ticketCodeField;
    @FXML private Button verifyButton;
    @FXML private VBox resultPanel;

    private final Map<String, Long> eventNameToId = new LinkedHashMap<>();
    private StompWebSocketClient wsClient;
    private Long selectedEventId;
    private int totalCapacity;

    @FXML
    public void initialize() {
        loadEvents();

        // Auto-focus on ticket code
        Platform.runLater(() -> ticketCodeField.requestFocus());

        // Enter key triggers verify
        ticketCodeField.setOnAction(e -> onVerify());

        // Scene-wide key capture — redirect to input
        ticketCodeField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyTyped(e -> {
                    if (!ticketCodeField.isFocused()) {
                        ticketCodeField.requestFocus();
                    }
                });
            }
        });

        eventSelector.setOnAction(e -> onEventSelected());

        showAwaitingState();
    }

    @SuppressWarnings("unchecked")
    private void loadEvents() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                Long userId = SessionManager.getInstance().getUserId();
                Map<String, Object> page = ApiClient.getInstance().get(
                        "/api/events?organizerId=" + userId + "&status=UPCOMING&size=50", Map.class);
                if (page != null && page.containsKey("content")) {
                    return (List<Map<String, Object>>) page.get("content");
                }
                return Collections.emptyList();
            }
        };
        task.setOnSucceeded(e -> {
            List<Map<String, Object>> events = task.getValue();
            Platform.runLater(() -> {
                eventSelector.getItems().clear();
                eventNameToId.clear();
                for (Map<String, Object> ev : events) {
                    String title = ev.get("title") != null ? ev.get("title").toString() : "Untitled";
                    Long id = ev.get("id") instanceof Number ? ((Number) ev.get("id")).longValue() : null;
                    eventNameToId.put(title, id);
                    eventSelector.getItems().add(title);
                }
                if (!eventSelector.getItems().isEmpty()) {
                    eventSelector.setValue(eventSelector.getItems().get(0));
                    onEventSelected();
                }
            });
        });
        runBackground(task);
    }

    private void onEventSelected() {
        String selected = eventSelector.getValue();
        if (selected == null) return;
        selectedEventId = eventNameToId.get(selected);
        if (selectedEventId == null) return;

        // Disconnect old WS
        if (wsClient != null) wsClient.disconnect();

        // Connect to attendance topic
        String wsUrl = ApiClient.getInstance().getBaseUrl().replace("http", "ws") + "/ws";
        wsClient = new StompWebSocketClient();
        wsClient.connect(wsUrl);
        wsClient.subscribe("/topic/attendance/" + selectedEventId, this::onAttendanceUpdate);

        counterLabel.setText("Checked In: 0 / —");
        showAwaitingState();
    }

    @SuppressWarnings("unchecked")
    private void onAttendanceUpdate(String body) {
        try {
            Map<String, Object> data = ApiClient.getInstance().getMapper().readValue(body, Map.class);
            int checkedIn = data.get("checkedIn") instanceof Number ? ((Number) data.get("checkedIn")).intValue() : 0;
            int total = data.get("totalCapacity") instanceof Number ? ((Number) data.get("totalCapacity")).intValue() : 0;
            String lastAttendee = data.get("attendeeName") != null ? data.get("attendeeName").toString() : "";
            String tierName = data.get("tierName") != null ? data.get("tierName").toString() : "";
            totalCapacity = total;

            Platform.runLater(() -> {
                counterLabel.setText("Checked In: " + checkedIn + " / " + total);
                // Flash counter
                flashNode(counterLabel);
            });
        } catch (Exception e) {
            System.err.println("Failed to parse attendance update: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unchecked")
    private void onVerify() {
        String code = ticketCodeField.getText().trim();
        if (code.isEmpty()) return;
        if (selectedEventId == null) {
            showErrorState("Please select an event first.");
            return;
        }

        verifyButton.setDisable(true);

        Map<String, String> body = new HashMap<>();
        body.put("ticketCode", code);
        body.put("eventId", selectedEventId.toString());

        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                return ApiClient.getInstance().post("/api/tickets/verify", body, Map.class);
            }
        };

        task.setOnSucceeded(e -> {
            Map<String, Object> result = task.getValue();
            Platform.runLater(() -> {
                verifyButton.setDisable(false);
                String attendee = result != null && result.get("attendeeName") != null ? result.get("attendeeName").toString() : "Attendee";
                String tier = result != null && result.get("tierName") != null ? result.get("tierName").toString() : "";
                showSuccessState(attendee, tier);
                // Clear after 3 seconds
                scheduleInputClear(3000);
            });
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Verification failed";
            Platform.runLater(() -> {
                verifyButton.setDisable(false);
                showErrorState(msg);
                scheduleInputClear(2000);
            });
        });
        runBackground(task);
    }

    @FXML
    private void onClear() {
        ticketCodeField.clear();
        ticketCodeField.requestFocus();
        showAwaitingState();
    }

    // ── Result panel states ──

    private void showAwaitingState() {
        resultPanel.getChildren().clear();
        VBox card = createResultCard("#e9ecef", "#6c757d");
        Label icon = new Label("⏳");
        icon.setStyle("-fx-font-size: 48px;");
        Label msg = new Label("Awaiting scan...");
        msg.setStyle("-fx-font-size: 16px; -fx-text-fill: #6c757d;");
        card.getChildren().addAll(icon, msg);
        resultPanel.getChildren().add(card);
    }

    private void showSuccessState(String attendeeName, String tierName) {
        resultPanel.getChildren().clear();
        VBox card = createResultCard("#d4edda", "#155724");
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 48px;");
        Label name = new Label(attendeeName);
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #155724;");
        Label tier = new Label(tierName);
        tier.setStyle("-fx-font-size: 14px; -fx-text-fill: #155724;");
        Label time = new Label("Checked in at " + java.time.LocalTime.now().withNano(0));
        time.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        card.getChildren().addAll(icon, name, tier, time);
        animateIn(card);
        resultPanel.getChildren().add(card);
    }

    private void showErrorState(String errorMsg) {
        resultPanel.getChildren().clear();
        VBox card = createResultCard("#f8d7da", "#721c24");
        Label icon = new Label("❌");
        icon.setStyle("-fx-font-size: 48px;");
        Label msg = new Label(errorMsg);
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #721c24; -fx-wrap-text: true;");
        msg.setWrapText(true);
        card.getChildren().addAll(icon, msg);
        animateIn(card);
        resultPanel.getChildren().add(card);
    }

    private VBox createResultCard(String bgColor, String borderColor) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; " +
                "-fx-border-color: " + borderColor + "; -fx-border-radius: 12;");
        return card;
    }

    // ── Animations ──

    private void animateIn(VBox card) {
        card.setOpacity(0);
        card.setTranslateY(20);
        FadeTransition fade = new FadeTransition(Duration.millis(300), card);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), card);
        slide.setFromY(20); slide.setToY(0);
        fade.play(); slide.play();
    }

    private void flashNode(Label node) {
        String original = node.getStyle();
        node.setStyle(original + " -fx-text-fill: #28a745;");
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> node.setStyle(original));
        }).start();
    }

    private void scheduleInputClear(int ms) {
        new Thread(() -> {
            try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                ticketCodeField.clear();
                ticketCodeField.requestFocus();
            });
        }).start();
    }

    public void cleanup() {
        if (wsClient != null) wsClient.disconnect();
    }

    private void runBackground(Task<?> task) {
        Thread t = new Thread(task); t.setDaemon(true); t.start();
    }
}
