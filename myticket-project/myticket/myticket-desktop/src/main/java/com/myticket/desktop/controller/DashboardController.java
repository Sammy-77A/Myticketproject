package com.myticket.desktop.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.myticket.common.enums.Role;
import com.myticket.desktop.util.ApiClient;
import com.myticket.desktop.util.DataReceiver;
import com.myticket.desktop.util.SceneManager;
import com.myticket.desktop.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Controller for the main dashboard screen (dashboard.fxml).
 */
public class DashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label pageTitle;
    @FXML private Label userEmailLabel;
    @FXML private Button bellButton;
    @FXML private Label badgeLabel;
    @FXML private Button navDashboard, navMyEvents, navCreateEvent, navScanner, navAnalytics, navProfile, navAdmin, navSettings;

    private Button activeNav;
    private ScheduledExecutorService notificationPoller;
    private TableView<Map<String, Object>> eventsTable;

    @FXML
    public void initialize() {
        // Set user email
        userEmailLabel.setText(SessionManager.getInstance().getUserEmail());

        // Show admin panel if ADMIN
        if (SessionManager.getInstance().hasRole(Role.ADMIN)) {
            navAdmin.setVisible(true);
            navAdmin.setManaged(true);
        }

        // Show profile nav for organizers
        if (!SessionManager.getInstance().hasRole(Role.ORGANIZER) && !SessionManager.getInstance().hasRole(Role.ADMIN)) {
            navProfile.setVisible(false);
            navProfile.setManaged(false);
        }

        activeNav = navDashboard;

        // Load default pane
        loadDashboardPane();

        // Start notification polling
        startNotificationPolling();
    }

    // ── Sidebar Navigation ──

    @FXML private void onNavDashboard()   { switchNav(navDashboard, "Dashboard"); loadDashboardPane(); }
    @FXML private void onNavMyEvents()    { switchNav(navMyEvents, "My Events"); loadDashboardPane(); }
    @FXML private void onNavCreateEvent() { switchNav(navCreateEvent, "Create Event"); loadEventForm(null); }
    @FXML private void onNavScanner()     { switchNav(navScanner, "Scanner"); loadFxmlPane("/fxml/scanner.fxml"); }
    @FXML private void onNavAnalytics()   { switchNav(navAnalytics, "Analytics"); loadFxmlPane("/fxml/analytics.fxml"); }
    @FXML private void onNavProfile()     { switchNav(navProfile, "My Profile"); loadProfilePane(); }
    @FXML private void onNavAdmin()       { switchNav(navAdmin, "Admin Panel"); loadFxmlPane("/fxml/admin.fxml"); }
    @FXML private void onNavSettings()    { switchNav(navSettings, "Settings"); loadSettingsPane(); }

    @FXML
    private void onLogout() {
        SessionManager.getInstance().clear();
        stopNotificationPolling();
        SceneManager.getInstance().navigateTo("/fxml/login.fxml");
    }

    private void switchNav(Button btn, String title) {
        if (activeNav != null) {
            activeNav.getStyleClass().remove("sidebar-item-active");
            if (!activeNav.getStyleClass().contains("sidebar-item")) activeNav.getStyleClass().add("sidebar-item");
        }
        btn.getStyleClass().remove("sidebar-item");
        if (!btn.getStyleClass().contains("sidebar-item-active")) btn.getStyleClass().add("sidebar-item-active");
        activeNav = btn;
        pageTitle.setText(title);
    }

    // ── Notification Bell ──

    @FXML
    private void onBellClick() {
        loadNotificationsPopup();
    }

    private void startNotificationPolling() {
        notificationPoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notif-poller");
            t.setDaemon(true);
            return t;
        });
        notificationPoller.scheduleAtFixedRate(this::fetchUnreadCount, 0, 30, TimeUnit.SECONDS);
    }

    private void stopNotificationPolling() {
        if (notificationPoller != null) notificationPoller.shutdownNow();
    }

    private void fetchUnreadCount() {
        try {
            Map<String, Object> result = ApiClient.getInstance().get("/api/notifications/unread-count", Map.class);
            if (result != null) {
                Object countObj = result.get("unreadCount");
                long count = countObj instanceof Number ? ((Number) countObj).longValue() : 0;
                Platform.runLater(() -> {
                    if (count > 0) {
                        badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
                        badgeLabel.setVisible(true);
                    } else {
                        badgeLabel.setVisible(false);
                    }
                });
            }
        } catch (Exception e) {
            // Silently fail — don't spam errors
        }
    }

    // ── Dashboard Pane ──

    private void loadDashboardPane() {
        VBox dashPane = new VBox(16);
        dashPane.setPadding(new Insets(4));

        // Stats cards row
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        VBox card1 = createStatCard("0", "Total Events");
        VBox card2 = createStatCard("0", "Tickets Issued");
        VBox card3 = createStatCard("0", "Upcoming Events");
        VBox card4 = createStatCard("0", "Active Waitlists");
        statsRow.getChildren().addAll(card1, card2, card3, card4);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        // Events table
        eventsTable = new TableView<>();
        eventsTable.setPlaceholder(new Label("Loading events..."));
        VBox.setVgrow(eventsTable, Priority.ALWAYS);

        TableColumn<Map<String, Object>, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "title")));
        titleCol.setPrefWidth(200);

        TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "eventDate")));
        dateCol.setPrefWidth(140);

        TableColumn<Map<String, Object>, String> venueCol = new TableColumn<>("Venue");
        venueCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "venue")));
        venueCol.setPrefWidth(160);

        TableColumn<Map<String, Object>, String> bookedCol = new TableColumn<>("Booked / Capacity");
        bookedCol.setCellValueFactory(cd -> {
            Map<String, Object> row = cd.getValue();
            int sold = intVal(row, "ticketsSold");
            int cap = intVal(row, "totalCapacity");
            return new SimpleStringProperty(sold + " / " + cap);
        });
        bookedCol.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "status")));
        statusCol.setPrefWidth(100);

        eventsTable.getColumns().addAll(titleCol, dateCol, venueCol, bookedCol, statusCol);

        // Row click → detail panel
        eventsTable.setRowFactory(tv -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    showEventDetail(row.getItem());
                }
            });
            return row;
        });

        dashPane.getChildren().addAll(statsRow, eventsTable);
        contentArea.getChildren().setAll(dashPane);

        // Fetch data
        fetchStats(card1, card2, card3, card4);
        fetchEvents();
    }

    private VBox createStatCard(String value, String label) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("stat-value");
        valLabel.setId("stat-" + label.replace(" ", "-").toLowerCase());
        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("stat-label");
        card.getChildren().addAll(valLabel, nameLabel);
        return card;
    }

    private void fetchStats(VBox card1, VBox card2, VBox card3, VBox card4) {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                if (SessionManager.getInstance().hasRole(Role.ADMIN)) {
                    return ApiClient.getInstance().get("/api/analytics/overview", Map.class);
                }
                return ApiClient.getInstance().get("/api/analytics/my-events", Map.class);
            }
        };
        task.setOnSucceeded(e -> {
            Map<String, Object> data = task.getValue();
            if (data != null) {
                Platform.runLater(() -> {
                    setStatValue(card1, String.valueOf(intVal(data, "totalEvents")));
                    setStatValue(card2, String.valueOf(intVal(data, "totalTickets")));
                    setStatValue(card3, String.valueOf(intVal(data, "upcomingEvents")));
                    setStatValue(card4, String.valueOf(intVal(data, "activeWaitlists")));
                });
            }
        });
        runBackground(task);
    }

    private void setStatValue(VBox card, String value) {
        if (!card.getChildren().isEmpty() && card.getChildren().get(0) instanceof Label lbl) {
            lbl.setText(value);
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchEvents() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                Long userId = SessionManager.getInstance().getUserId();
                String endpoint = "/api/events?organizerId=" + userId + "&size=50";
                Map<String, Object> page = ApiClient.getInstance().get(endpoint, Map.class);
                if (page != null && page.containsKey("content")) {
                    return (List<Map<String, Object>>) page.get("content");
                }
                return Collections.emptyList();
            }
        };
        task.setOnSucceeded(e -> {
            List<Map<String, Object>> events = task.getValue();
            Platform.runLater(() -> {
                ObservableList<Map<String, Object>> items = FXCollections.observableArrayList(events);
                eventsTable.setItems(items);
                if (events.isEmpty()) eventsTable.setPlaceholder(new Label("No events found. Create your first event!"));
            });
        });
        task.setOnFailed(e -> Platform.runLater(() -> eventsTable.setPlaceholder(new Label("Could not load events."))));
        runBackground(task);
    }

    // ── Event Detail Panel ──

    private void showEventDetail(Map<String, Object> event) {
        VBox detail = new VBox(12);
        detail.getStyleClass().add("detail-panel");
        detail.setPrefWidth(300);
        detail.setMaxWidth(300);

        Label title = new Label(str(event, "title"));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label date = new Label("📅 " + str(event, "eventDate"));
        Label venue = new Label("📍 " + str(event, "venue"));
        Label status = new Label("Status: " + str(event, "status"));

        Label booked = new Label("Booked: " + intVal(event, "ticketsSold") + " / " + intVal(event, "totalCapacity"));

        Separator sep = new Separator();

        Button editBtn = new Button("✏️ Edit Event");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        editBtn.setOnAction(e -> loadEventForm(event));

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> loadDashboardPane());

        detail.getChildren().addAll(title, date, venue, status, booked, sep, editBtn, closeBtn);

        // Show as right panel using HBox
        Node mainContent = contentArea.getChildren().get(0);
        HBox split = new HBox();
        HBox.setHgrow(mainContent, Priority.ALWAYS);
        split.getChildren().addAll(mainContent, detail);
        contentArea.getChildren().setAll(split);
    }

    // ── Notifications Popup ──

    @SuppressWarnings("unchecked")
    private void loadNotificationsPopup() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Notifications");
        dialog.setHeaderText(null);

        VBox content = new VBox(8);
        content.setPrefWidth(380);
        content.setPrefHeight(400);
        content.getStyleClass().add("notification-popup");

        Button markAllBtn = new Button("Mark all read");
        markAllBtn.getStyleClass().add("btn-secondary");
        markAllBtn.setOnAction(e -> {
            Task<Void> markTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiClient.getInstance().put("/api/notifications/read-all", null, Void.class);
                    return null;
                }
            };
            markTask.setOnSucceeded(ev -> {
                fetchUnreadCount();
                dialog.close();
            });
            runBackground(markTask);
        });

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(350);
        VBox notifList = new VBox(0);
        scroll.setContent(notifList);
        notifList.getChildren().add(new Label("Loading..."));

        content.getChildren().addAll(markAllBtn, scroll);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.show();

        // Fetch notifications
        Task<List<Map<String, Object>>> fetchTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                Map<String, Object> page = ApiClient.getInstance().get("/api/notifications?size=20", Map.class);
                if (page != null && page.containsKey("content")) {
                    return (List<Map<String, Object>>) page.get("content");
                }
                return Collections.emptyList();
            }
        };
        fetchTask.setOnSucceeded(e -> {
            List<Map<String, Object>> notifs = fetchTask.getValue();
            Platform.runLater(() -> {
                notifList.getChildren().clear();
                if (notifs.isEmpty()) {
                    notifList.getChildren().add(new Label("No notifications."));
                } else {
                    for (Map<String, Object> n : notifs) {
                        HBox item = new HBox(8);
                        item.getStyleClass().add("notification-item");
                        boolean isRead = Boolean.TRUE.equals(n.get("read"));
                        if (!isRead) item.getStyleClass().add("notification-unread");

                        Label msg = new Label(str(n, "message"));
                        msg.setWrapText(true);
                        msg.setMaxWidth(300);

                        Label time = new Label(str(n, "createdAt"));
                        time.getStyleClass().add("stat-label");
                        time.setMinWidth(80);

                        item.getChildren().addAll(msg, time);

                        item.setOnMouseClicked(ev -> {
                            Object eventId = n.get("relatedEventId");
                            if (eventId != null) {
                                dialog.close();
                                // Could navigate to event detail
                            }
                        });

                        notifList.getChildren().add(item);
                    }
                }
            });
        });
        runBackground(fetchTask);
    }

    // ── Event Form ──

    private void loadEventForm(Map<String, Object> eventData) {
        SceneManager.getInstance().navigateTo("/fxml/event-form.fxml", eventData);
    }

    // ── Profile Pane ──

    private void loadProfilePane() {
        VBox profilePane = new VBox(16);
        profilePane.setAlignment(Pos.TOP_CENTER);
        profilePane.setPadding(new Insets(20));

        VBox card = new VBox(12);
        card.getStyleClass().add("profile-card");
        card.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("My Profile");
        header.getStyleClass().add("section-header");

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        nameField.getStyleClass().add("text-field");

        TextArea bioField = new TextArea();
        bioField.setPromptText("Bio");
        bioField.setPrefRowCount(3);

        Label followersLabel = new Label("Followers: loading...");
        followersLabel.getStyleClass().add("stat-label");

        Button saveBtn = new Button("Save Profile");
        saveBtn.getStyleClass().add("btn-primary");

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("error-label");
        statusLabel.setVisible(false);

        card.getChildren().addAll(header, new Label("Full Name"), nameField, new Label("Bio"), bioField, followersLabel, saveBtn, statusLabel);
        profilePane.getChildren().add(card);
        contentArea.getChildren().setAll(profilePane);

        // Load current profile
        Task<Map<String, Object>> loadTask = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                return ApiClient.getInstance().get("/api/users/me", Map.class);
            }
        };
        loadTask.setOnSucceeded(e -> {
            Map<String, Object> user = loadTask.getValue();
            Platform.runLater(() -> {
                nameField.setText(str(user, "fullName"));
                bioField.setText(str(user, "bio"));
            });
        });
        runBackground(loadTask);

        // Load follower count
        Task<Void> followerTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Long userId = SessionManager.getInstance().getUserId();
                try {
                    Map<String, Object> profile = ApiClient.getInstance().get("/api/organizers/" + userId + "/profile", Map.class);
                    if (profile != null) {
                        Platform.runLater(() -> followersLabel.setText("Followers: " + intVal(profile, "followerCount")));
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> followersLabel.setText("Followers: 0"));
                }
                return null;
            }
        };
        runBackground(followerTask);

        // Save handler
        saveBtn.setOnAction(e -> {
            Map<String, String> body = new HashMap<>();
            body.put("fullName", nameField.getText());
            body.put("bio", bioField.getText());
            Task<Void> saveTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiClient.getInstance().put("/api/users/me", body, Map.class);
                    return null;
                }
            };
            saveTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                statusLabel.setStyle("-fx-text-fill: #28a745;");
                statusLabel.setText("Profile saved!");
                statusLabel.setVisible(true);
            }));
            saveTask.setOnFailed(ev -> Platform.runLater(() -> {
                statusLabel.setStyle("-fx-text-fill: #dc3545;");
                statusLabel.setText("Failed to save profile.");
                statusLabel.setVisible(true);
            }));
            runBackground(saveTask);
        });
    }

    // ── FXML sub-pane loader ──

    private void loadFxmlPane(String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent pane = loader.load();
            contentArea.getChildren().setAll(pane);
        } catch (Exception e) {
            System.err.println("Failed to load " + fxmlPath + ": " + e.getMessage());
            loadPlaceholder("Could not load screen.");
        }
    }

    // ── Settings Pane ──

    private void loadSettingsPane() {
        VBox settingsPane = new VBox(16);
        settingsPane.setAlignment(Pos.TOP_CENTER);
        settingsPane.setPadding(new Insets(20));

        // Profile card
        VBox profileCard = new VBox(12);
        profileCard.getStyleClass().add("profile-card");
        profileCard.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("Account Settings");
        header.getStyleClass().add("section-header");

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        Label emailLabel = new Label("Email: " + SessionManager.getInstance().getUserEmail());
        emailLabel.getStyleClass().add("stat-label");
        Label roleLabel = new Label("Role: " + SessionManager.getInstance().getRole());
        roleLabel.getStyleClass().add("stat-label");

        Button saveProfileBtn = new Button("Save Profile");
        saveProfileBtn.getStyleClass().add("btn-primary");
        Label profileStatus = new Label();
        profileStatus.setVisible(false);

        profileCard.getChildren().addAll(header, new Label("Full Name"), nameField, emailLabel, roleLabel, saveProfileBtn, profileStatus);

        // Change password card
        VBox pwCard = new VBox(12);
        pwCard.getStyleClass().add("profile-card");
        pwCard.setAlignment(Pos.CENTER_LEFT);

        Label pwHeader = new Label("Change Password");
        pwHeader.getStyleClass().add("section-header");

        PasswordField currentPw = new PasswordField();
        currentPw.setPromptText("Current password");
        PasswordField newPw = new PasswordField();
        newPw.setPromptText("New password");
        PasswordField confirmPw = new PasswordField();
        confirmPw.setPromptText("Confirm new password");

        Button changePwBtn = new Button("Change Password");
        changePwBtn.getStyleClass().add("btn-primary");
        Label pwStatus = new Label();
        pwStatus.setVisible(false);

        pwCard.getChildren().addAll(pwHeader, new Label("Current Password"), currentPw, new Label("New Password"), newPw, new Label("Confirm Password"), confirmPw, changePwBtn, pwStatus);

        // Logout
        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.getStyleClass().add("btn-danger");
        logoutBtn.setOnAction(e -> onLogout());

        settingsPane.getChildren().addAll(profileCard, pwCard, logoutBtn);
        contentArea.getChildren().setAll(settingsPane);

        // Load current user
        Task<Map<String, Object>> loadTask = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                return ApiClient.getInstance().get("/api/users/me", Map.class);
            }
        };
        loadTask.setOnSucceeded(e -> {
            Map<String, Object> user = loadTask.getValue();
            Platform.runLater(() -> nameField.setText(str(user, "fullName")));
        });
        runBackground(loadTask);

        // Save profile
        saveProfileBtn.setOnAction(e -> {
            Map<String, String> body = new HashMap<>();
            body.put("fullName", nameField.getText());
            Task<Void> saveTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiClient.getInstance().put("/api/users/me", body, Map.class);
                    return null;
                }
            };
            saveTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                profileStatus.setStyle("-fx-text-fill: #28a745;");
                profileStatus.setText("Profile saved!");
                profileStatus.setVisible(true);
            }));
            saveTask.setOnFailed(ev -> Platform.runLater(() -> {
                profileStatus.setStyle("-fx-text-fill: #dc3545;");
                profileStatus.setText("Save failed.");
                profileStatus.setVisible(true);
            }));
            runBackground(saveTask);
        });

        // Change password
        changePwBtn.setOnAction(e -> {
            if (!newPw.getText().equals(confirmPw.getText())) {
                pwStatus.setStyle("-fx-text-fill: #dc3545;");
                pwStatus.setText("Passwords do not match.");
                pwStatus.setVisible(true);
                return;
            }
            Map<String, String> body = new HashMap<>();
            body.put("oldPassword", currentPw.getText());
            body.put("newPassword", newPw.getText());
            Task<Void> pwTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiClient.getInstance().put("/api/auth/change-password", body, Map.class);
                    return null;
                }
            };
            pwTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                pwStatus.setStyle("-fx-text-fill: #28a745;");
                pwStatus.setText("Password changed!");
                pwStatus.setVisible(true);
                currentPw.clear(); newPw.clear(); confirmPw.clear();
            }));
            pwTask.setOnFailed(ev -> Platform.runLater(() -> {
                pwStatus.setStyle("-fx-text-fill: #dc3545;");
                pwStatus.setText("Password change failed.");
                pwStatus.setVisible(true);
            }));
            runBackground(pwTask);
        });
    }

    // ── Placeholder ──

    private void loadPlaceholder(String text) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 18px; -fx-text-fill: #6c757d;");
        box.getChildren().add(lbl);
        contentArea.getChildren().setAll(box);
    }

    // ── Helpers ──

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private int intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private void runBackground(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
