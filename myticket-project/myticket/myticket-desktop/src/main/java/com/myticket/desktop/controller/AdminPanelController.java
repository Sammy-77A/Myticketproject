package com.myticket.desktop.controller;

import com.myticket.desktop.util.ApiClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Admin panel controller — Users, Audit Log, Subscribers tabs.
 */
public class AdminPanelController {

    @FXML private TabPane tabPane;

    // Users tab
    @FXML private TableView<Map<String, Object>> usersTable;
    @FXML private TableColumn<Map<String, Object>, String> userEmailCol;
    @FXML private TableColumn<Map<String, Object>, String> userNameCol;
    @FXML private TableColumn<Map<String, Object>, String> userRoleCol;
    @FXML private TableColumn<Map<String, Object>, String> userVerifiedCol;
    @FXML private TableColumn<Map<String, Object>, String> userCreatedCol;

    // Audit tab
    @FXML private TableView<Map<String, Object>> auditTable;
    @FXML private TableColumn<Map<String, Object>, String> auditTimestampCol;
    @FXML private TableColumn<Map<String, Object>, String> auditActorCol;
    @FXML private TableColumn<Map<String, Object>, String> auditActionCol;
    @FXML private TableColumn<Map<String, Object>, String> auditEntityCol;
    @FXML private TableColumn<Map<String, Object>, String> auditDetailCol;
    @FXML private TextField auditFilterAction;
    @FXML private TextField auditFilterActor;

    // Subscribers tab
    @FXML private TableView<Map<String, Object>> subscribersTable;
    @FXML private TableColumn<Map<String, Object>, String> subEmailCol;
    @FXML private TableColumn<Map<String, Object>, String> subDateCol;
    @FXML private Label subCountLabel;

    private int auditPage = 0;

    @FXML
    public void initialize() {
        // Users columns
        userEmailCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "email")));
        userNameCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "fullName")));
        userRoleCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "role")));
        userVerifiedCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "verified")));
        userCreatedCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "createdAt")));

        // Audit columns
        auditTimestampCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "timestamp")));
        auditActorCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "actorEmail")));
        auditActionCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "action")));
        auditEntityCol.setCellValueFactory(cd -> new SimpleStringProperty(
                str(cd.getValue(), "entityType") + " #" + str(cd.getValue(), "entityId")));
        auditDetailCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "detail")));

        // Subscribers columns
        subEmailCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "email")));
        subDateCol.setCellValueFactory(cd -> new SimpleStringProperty(str(cd.getValue(), "subscribedAt")));

        // Add role change column to users
        TableColumn<Map<String, Object>, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(150);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final ChoiceBox<String> roleBox = new ChoiceBox<>(FXCollections.observableArrayList("STUDENT", "ORGANIZER", "ADMIN"));
            private final Button saveBtn = new Button("Save");
            {
                saveBtn.getStyleClass().add("btn-secondary");
                saveBtn.setStyle("-fx-font-size: 11px;");
                saveBtn.setOnAction(e -> {
                    Map<String, Object> user = getTableView().getItems().get(getIndex());
                    String newRole = roleBox.getValue();
                    if (newRole != null) confirmRoleChange(user, newRole);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Map<String, Object> user = getTableView().getItems().get(getIndex());
                roleBox.setValue(str(user, "role"));
                HBox box = new HBox(4, roleBox, saveBtn);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        usersTable.getColumns().add(actionCol);

        // Load data
        loadUsers();
        loadAuditLog();
        loadSubscribers();
    }

    // ── Users ──

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return ApiClient.getInstance().get("/api/admin/users", List.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() ->
                usersTable.setItems(FXCollections.observableArrayList(task.getValue()))));
        task.setOnFailed(e -> Platform.runLater(() ->
                usersTable.setPlaceholder(new Label("Failed to load users."))));
        runBackground(task);
    }

    private void confirmRoleChange(Map<String, Object> user, String newRole) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Change " + str(user, "email") + " role to " + newRole + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Role Change");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                Object idObj = user.get("id");
                Long id = idObj instanceof Number ? ((Number) idObj).longValue() : null;
                if (id == null) return;
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        ApiClient.getInstance().put("/api/admin/users/" + id + "/role",
                                Map.of("role", newRole), Map.class);
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(this::loadUsers));
                task.setOnFailed(e -> Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Role change failed.");
                    alert.showAndWait();
                }));
                runBackground(task);
            }
        });
    }

    // ── Audit Log ──

    @FXML
    @SuppressWarnings("unchecked")
    public void onFilterAudit() {
        auditPage = 0;
        loadAuditLog();
    }

    @FXML
    public void onLoadMoreAudit() {
        auditPage++;
        loadAuditLog();
    }

    @SuppressWarnings("unchecked")
    private void loadAuditLog() {
        String action = auditFilterAction != null ? auditFilterAction.getText().trim() : "";
        String actor = auditFilterActor != null ? auditFilterActor.getText().trim() : "";
        String params = "?page=" + auditPage + "&size=50";
        if (!action.isEmpty()) params += "&action=" + action;
        if (!actor.isEmpty()) params += "&actorEmail=" + actor;

        String endpoint = "/api/admin/audit-log" + params;
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                Object result = ApiClient.getInstance().get(endpoint, Map.class);
                if (result instanceof Map) {
                    Map<String, Object> page = (Map<String, Object>) result;
                    if (page.containsKey("content")) {
                        return (List<Map<String, Object>>) page.get("content");
                    }
                }
                // Maybe it's a direct list
                return ApiClient.getInstance().get(endpoint, List.class);
            }
        };
        task.setOnSucceeded(e -> {
            List<Map<String, Object>> logs = task.getValue();
            Platform.runLater(() -> {
                if (auditPage == 0) {
                    auditTable.setItems(FXCollections.observableArrayList(logs != null ? logs : Collections.emptyList()));
                } else if (logs != null) {
                    auditTable.getItems().addAll(logs);
                }
            });
        });
        runBackground(task);
    }

    // ── Subscribers ──

    @SuppressWarnings("unchecked")
    private void loadSubscribers() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return ApiClient.getInstance().get("/api/admin/subscribers", List.class);
            }
        };
        task.setOnSucceeded(e -> {
            List<Map<String, Object>> subs = task.getValue();
            Platform.runLater(() -> {
                subscribersTable.setItems(FXCollections.observableArrayList(subs != null ? subs : Collections.emptyList()));
                subCountLabel.setText("Total: " + (subs != null ? subs.size() : 0));
            });
        });
        runBackground(task);
    }

    // ── helpers ──
    private String str(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? v.toString() : ""; }
    private void runBackground(Task<?> t) { Thread th = new Thread(t); th.setDaemon(true); th.start(); }
}
