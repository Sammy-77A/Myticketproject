package com.myticket.desktop.controller;

import com.myticket.desktop.util.ApiClient;
import com.myticket.desktop.util.DataReceiver;
import com.myticket.desktop.util.SceneManager;
import com.myticket.desktop.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller for the Event creation/editing form (event-form.fxml).
 * Implements DataReceiver to accept event data for edit mode.
 */
public class EventFormController implements DataReceiver {

    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ChoiceBox<String> categoryChoice;
    @FXML private TextField venueField;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField eventTimeField;
    @FXML private Spinner<Integer> capacitySpinner;
    @FXML private Spinner<Integer> minAgeSpinner;
    @FXML private VBox tiersContainer;
    @FXML private VBox performersContainer;
    @FXML private ImageView bannerPreview;
    @FXML private Label bannerFileName;
    @FXML private Label errorLabel;
    @FXML private Button publishButton;

    private Map<String, Object> editingEvent;
    private Long editingEventId;
    private File selectedBannerFile;
    private final List<Map<String, Object>> tierRows = new ArrayList<>();
    private final List<Map<String, Object>> performerRows = new ArrayList<>();
    private final Map<Long, String> categoryMap = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        // Spinners
        capacitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 100));
        minAgeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 99, 0));

        // Load categories
        loadCategories();

        // Add a default tier row
        onAddTier();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initData(Object data) {
        if (data instanceof Map) {
            editingEvent = (Map<String, Object>) data;
            editingEventId = longVal(editingEvent, "id");
            formTitle.setText("Edit Event");
            publishButton.setText("Update Event");

            // Pre-populate fields
            titleField.setText(str(editingEvent, "title"));
            descriptionField.setText(str(editingEvent, "description"));
            venueField.setText(str(editingEvent, "venue"));
            eventTimeField.setText(extractTime(str(editingEvent, "eventDate")));
            try {
                String dateStr = str(editingEvent, "eventDate");
                if (dateStr.length() >= 10) {
                    eventDatePicker.setValue(LocalDate.parse(dateStr.substring(0, 10)));
                }
            } catch (Exception ignored) {}

            capacitySpinner.getValueFactory().setValue(intVal(editingEvent, "totalCapacity"));
            minAgeSpinner.getValueFactory().setValue(intVal(editingEvent, "minAge"));
        }
    }

    // ── Category Loading ──

    @SuppressWarnings("unchecked")
    private void loadCategories() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return ApiClient.getInstance().get("/api/categories", List.class);
            }
        };
        task.setOnSucceeded(e -> {
            List<Map<String, Object>> cats = task.getValue();
            Platform.runLater(() -> {
                categoryChoice.getItems().clear();
                if (cats != null) {
                    for (Map<String, Object> cat : cats) {
                        String name = str(cat, "name");
                        Long id = longVal(cat, "id");
                        categoryMap.put(id, name);
                        categoryChoice.getItems().add(name);
                    }
                }
                // Pre-select on edit
                if (editingEvent != null) {
                    String catName = str(editingEvent, "categoryName");
                    if (!catName.isEmpty()) categoryChoice.setValue(catName);
                }
            });
        });
        runBackground(task);
    }

    // ── Tier Management ──

    @FXML
    public void onAddTier() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4));
        row.setStyle("-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8;");

        TextField nameField = new TextField();
        nameField.setPromptText("Tier name");
        nameField.setPrefWidth(120);

        Spinner<Integer> priceSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000000, 0, 50));
        priceSpinner.setPrefWidth(100);
        priceSpinner.setEditable(true);

        Spinner<Integer> capSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 50));
        capSpinner.setPrefWidth(90);
        capSpinner.setEditable(true);

        TextField perksField = new TextField();
        perksField.setPromptText("Perks (comma-sep)");
        perksField.setPrefWidth(140);

        CheckBox earlyBird = new CheckBox("Early Bird");
        DatePicker closesAt = new DatePicker();
        closesAt.setPrefWidth(130);
        closesAt.setVisible(false);
        closesAt.setManaged(false);

        earlyBird.setOnAction(e -> {
            closesAt.setVisible(earlyBird.isSelected());
            closesAt.setManaged(earlyBird.isSelected());
        });

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setOnAction(e -> tiersContainer.getChildren().remove(row));

        row.getChildren().addAll(
                new Label("Name:"), nameField,
                new Label("Price:"), priceSpinner,
                new Label("Cap:"), capSpinner,
                perksField, earlyBird, closesAt, removeBtn
        );

        // Store references
        Map<String, Object> tierData = new HashMap<>();
        tierData.put("row", row);
        tierData.put("name", nameField);
        tierData.put("price", priceSpinner);
        tierData.put("capacity", capSpinner);
        tierData.put("perks", perksField);
        tierData.put("earlyBird", earlyBird);
        tierData.put("closesAt", closesAt);
        tierRows.add(tierData);

        tiersContainer.getChildren().add(row);
    }

    // ── Performer Management ──

    @FXML
    public void onAddPerformer() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4));
        row.setStyle("-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8;");

        TextField nameField = new TextField();
        nameField.setPromptText("Performer name");
        nameField.setPrefWidth(160);

        TextArea bioField = new TextArea();
        bioField.setPromptText("Short bio");
        bioField.setPrefRowCount(2);
        bioField.setPrefWidth(200);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setOnAction(e -> performersContainer.getChildren().remove(row));

        row.getChildren().addAll(new Label("Name:"), nameField, new Label("Bio:"), bioField, removeBtn);

        Map<String, Object> perfData = new HashMap<>();
        perfData.put("row", row);
        perfData.put("name", nameField);
        perfData.put("bio", bioField);
        performerRows.add(perfData);

        performersContainer.getChildren().add(row);
    }

    // ── Media ──

    @FXML
    public void onChooseBanner() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Banner Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(bannerPreview.getScene().getWindow());
        if (file != null) {
            selectedBannerFile = file;
            bannerFileName.setText(file.getName());
            bannerPreview.setImage(new Image(file.toURI().toString()));
        }
    }

    // ── Preview ──

    @FXML
    public void onPreview() {
        Alert preview = new Alert(Alert.AlertType.INFORMATION);
        preview.setTitle("Event Preview");
        preview.setHeaderText(titleField.getText());

        StringBuilder sb = new StringBuilder();
        sb.append("📍 ").append(venueField.getText()).append("\n");
        sb.append("📅 ").append(eventDatePicker.getValue()).append(" ").append(eventTimeField.getText()).append("\n");
        sb.append("🎟 Capacity: ").append(capacitySpinner.getValue()).append("\n");
        if (minAgeSpinner.getValue() > 0) sb.append("🔞 Min Age: ").append(minAgeSpinner.getValue()).append("\n");
        sb.append("\n").append(descriptionField.getText()).append("\n\n");

        sb.append("Tiers:\n");
        for (Map<String, Object> tier : tierRows) {
            HBox row = (HBox) tier.get("row");
            if (row.getParent() != null) { // still in container
                TextField n = (TextField) tier.get("name");
                Spinner<Integer> p = (Spinner<Integer>) tier.get("price");
                Spinner<Integer> c = (Spinner<Integer>) tier.get("capacity");
                sb.append("  • ").append(n.getText()).append(" — KES ").append(p.getValue()).append(" (").append(c.getValue()).append(" seats)\n");
            }
        }

        preview.setContentText(sb.toString());
        preview.showAndWait();
    }

    // ── Publish / Update ──

    @FXML
    @SuppressWarnings("unchecked")
    public void onPublish() {
        // Validate
        if (titleField.getText().trim().isEmpty()) {
            showError("Title is required."); return;
        }
        if (venueField.getText().trim().isEmpty()) {
            showError("Venue is required."); return;
        }
        if (eventDatePicker.getValue() == null) {
            showError("Date is required."); return;
        }
        if (tiersContainer.getChildren().isEmpty()) {
            showError("At least one ticket tier is required."); return;
        }

        hideError();
        publishButton.setDisable(true);
        publishButton.setText("Saving…");

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("title", titleField.getText().trim());
        body.put("description", descriptionField.getText().trim());
        body.put("venue", venueField.getText().trim());
        body.put("totalCapacity", capacitySpinner.getValue());
        body.put("minAge", minAgeSpinner.getValue());

        // Category
        String selectedCat = categoryChoice.getValue();
        if (selectedCat != null) {
            for (Map.Entry<Long, String> entry : categoryMap.entrySet()) {
                if (entry.getValue().equals(selectedCat)) {
                    body.put("categoryId", entry.getKey());
                    break;
                }
            }
        }

        // Date + time
        String time = eventTimeField.getText().trim();
        if (time.isEmpty()) time = "00:00";
        String dateTime = eventDatePicker.getValue().toString() + "T" + time + ":00";
        body.put("eventDate", dateTime);

        // Tiers
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (Map<String, Object> tierData : tierRows) {
            HBox row = (HBox) tierData.get("row");
            if (row.getParent() == null) continue; // removed
            TextField n = (TextField) tierData.get("name");
            Spinner<Integer> p = (Spinner<Integer>) tierData.get("price");
            Spinner<Integer> c = (Spinner<Integer>) tierData.get("capacity");
            TextField pk = (TextField) tierData.get("perks");
            CheckBox eb = (CheckBox) tierData.get("earlyBird");
            DatePicker ca = (DatePicker) tierData.get("closesAt");

            Map<String, Object> tier = new HashMap<>();
            tier.put("name", n.getText().trim());
            tier.put("price", p.getValue());
            tier.put("capacity", c.getValue());
            tier.put("perks", pk.getText().trim());
            tier.put("isEarlyBird", eb.isSelected());
            if (eb.isSelected() && ca.getValue() != null) {
                tier.put("closesAt", ca.getValue().toString() + "T23:59:59");
            }
            tiers.add(tier);
        }
        body.put("tiers", tiers);

        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                if (editingEventId != null) {
                    return ApiClient.getInstance().put("/api/events/" + editingEventId, body, Map.class);
                } else {
                    return ApiClient.getInstance().post("/api/events", body, Map.class);
                }
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            publishButton.setDisable(false);
            publishButton.setText(editingEventId != null ? "Update Event" : "Publish Event");
            // Navigate back to dashboard
            SceneManager.getInstance().navigateTo("/fxml/dashboard.fxml");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            publishButton.setDisable(false);
            publishButton.setText(editingEventId != null ? "Update Event" : "Publish Event");
            Throwable ex = task.getException();
            showError(ex != null ? ex.getMessage() : "Failed to save event.");
        }));

        runBackground(task);
    }

    @FXML
    public void onBack() {
        SceneManager.getInstance().navigateTo("/fxml/dashboard.fxml");
    }

    // ── Helpers ──

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private int intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private Long longVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return null;
    }

    private String extractTime(String dateTimeStr) {
        if (dateTimeStr != null && dateTimeStr.contains("T")) {
            String time = dateTimeStr.substring(dateTimeStr.indexOf("T") + 1);
            if (time.length() >= 5) return time.substring(0, 5);
        }
        return "18:00";
    }

    private void runBackground(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
