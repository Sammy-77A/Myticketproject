package com.myticket.desktop.controller;

import com.myticket.common.enums.Role;
import com.myticket.desktop.util.ApiClient;
import com.myticket.desktop.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;

/**
 * Analytics screen — charts and CSV export.
 */
public class AnalyticsScreenController {

    @FXML private ChoiceBox<String> eventSelector;
    @FXML private Button loadButton;
    @FXML private VBox chartsContainer;
    @FXML private HBox summaryBar;

    private final Map<String, Long> eventNameToId = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        loadEvents();
    }

    @SuppressWarnings("unchecked")
    private void loadEvents() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                Long userId = SessionManager.getInstance().getUserId();
                Map<String, Object> page = ApiClient.getInstance().get(
                        "/api/events?organizerId=" + userId + "&size=100", Map.class);
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
                for (Map<String, Object> ev : events) {
                    String title = str(ev, "title");
                    Long id = ev.get("id") instanceof Number ? ((Number) ev.get("id")).longValue() : null;
                    eventNameToId.put(title, id);
                    eventSelector.getItems().add(title);
                }
                if (!eventSelector.getItems().isEmpty()) {
                    eventSelector.setValue(eventSelector.getItems().get(0));
                }
            });
        });
        runBackground(task);
    }

    @FXML
    @SuppressWarnings("unchecked")
    public void onLoad() {
        String selected = eventSelector.getValue();
        if (selected == null) return;
        Long eventId = eventNameToId.get(selected);
        if (eventId == null) return;

        chartsContainer.getChildren().clear();
        summaryBar.getChildren().clear();
        chartsContainer.getChildren().add(new Label("Loading analytics..."));

        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                return ApiClient.getInstance().get("/api/analytics/events/" + eventId, Map.class);
            }
        };
        task.setOnSucceeded(e -> {
            Map<String, Object> data = task.getValue();
            Platform.runLater(() -> buildCharts(data, selected, eventId));
        });
        task.setOnFailed(e -> Platform.runLater(() -> {
            chartsContainer.getChildren().clear();
            chartsContainer.getChildren().add(new Label("Failed to load analytics."));
        }));
        runBackground(task);
    }

    @SuppressWarnings("unchecked")
    private void buildCharts(Map<String, Object> data, String eventTitle, Long eventId) {
        chartsContainer.getChildren().clear();
        summaryBar.getChildren().clear();

        // Row 1: LineChart + BarChart
        HBox row1 = new HBox(16);
        row1.setPrefHeight(280);

        // LineChart — bookings over time
        CategoryAxis xAxis1 = new CategoryAxis();
        xAxis1.setLabel("Date");
        NumberAxis yAxis1 = new NumberAxis();
        yAxis1.setLabel("Cumulative Bookings");
        LineChart<String, Number> lineChart = new LineChart<>(xAxis1, yAxis1);
        lineChart.setTitle("Bookings Over Time");
        lineChart.setLegendVisible(false);
        HBox.setHgrow(lineChart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(eventTitle);
        Object botObj = data.get("bookingsOverTime");
        if (botObj instanceof Map) {
            Map<String, Object> bot = (Map<String, Object>) botObj;
            List<Map.Entry<String, Object>> sorted = new ArrayList<>(bot.entrySet());
            sorted.sort(Comparator.comparing(Map.Entry::getKey));
            for (Map.Entry<String, Object> entry : sorted) {
                int val = entry.getValue() instanceof Number ? ((Number) entry.getValue()).intValue() : 0;
                series.getData().add(new XYChart.Data<>(entry.getKey(), val));
            }
        } else if (botObj instanceof List) {
            List<Map<String, Object>> botList = (List<Map<String, Object>>) botObj;
            for (Map<String, Object> item : botList) {
                String date = str(item, "date");
                int count = intVal(item, "count");
                series.getData().add(new XYChart.Data<>(date, count));
            }
        }
        lineChart.getData().add(series);

        // BarChart — tier breakdown
        CategoryAxis xAxis2 = new CategoryAxis();
        xAxis2.setLabel("Tier");
        NumberAxis yAxis2 = new NumberAxis();
        yAxis2.setLabel("Tickets Sold");
        BarChart<String, Number> barChart = new BarChart<>(xAxis2, yAxis2);
        barChart.setTitle("Tier Breakdown");
        barChart.setLegendVisible(false);
        HBox.setHgrow(barChart, Priority.ALWAYS);

        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        Object tbObj = data.get("tierBreakdown");
        if (tbObj instanceof Map) {
            Map<String, Object> tb = (Map<String, Object>) tbObj;
            for (Map.Entry<String, Object> entry : tb.entrySet()) {
                int val = entry.getValue() instanceof Number ? ((Number) entry.getValue()).intValue() : 0;
                barSeries.getData().add(new XYChart.Data<>(entry.getKey(), val));
            }
        } else if (tbObj instanceof List) {
            List<Map<String, Object>> tbList = (List<Map<String, Object>>) tbObj;
            for (Map<String, Object> item : tbList) {
                barSeries.getData().add(new XYChart.Data<>(str(item, "tierName"), intVal(item, "ticketsSold")));
            }
        }
        barChart.getData().add(barSeries);

        row1.getChildren().addAll(lineChart, barChart);

        // Row 2: PieCharts
        HBox row2 = new HBox(16);
        row2.setPrefHeight(260);

        // Attendance PieChart
        PieChart attendancePie = new PieChart();
        attendancePie.setTitle("Attendance");
        Object arObj = data.get("attendanceRate");
        if (arObj instanceof Map) {
            Map<String, Object> ar = (Map<String, Object>) arObj;
            for (Map.Entry<String, Object> entry : ar.entrySet()) {
                int val = entry.getValue() instanceof Number ? ((Number) entry.getValue()).intValue() : 0;
                if (val > 0) attendancePie.getData().add(new PieChart.Data(entry.getKey(), val));
            }
        }
        HBox.setHgrow(attendancePie, Priority.ALWAYS);

        // Right side
        if (SessionManager.getInstance().hasRole(Role.ADMIN)) {
            PieChart categoryPie = new PieChart();
            categoryPie.setTitle("Category Breakdown");
            HBox.setHgrow(categoryPie, Priority.ALWAYS);
            row2.getChildren().addAll(attendancePie, categoryPie);
        } else {
            VBox summaryCard = new VBox(8);
            summaryCard.getStyleClass().add("stat-card");
            summaryCard.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(summaryCard, Priority.ALWAYS);

            summaryCard.getChildren().addAll(
                    makeStatLine("Total Revenue", "KES " + intVal(data, "totalRevenue")),
                    makeStatLine("Average Rating", String.format("%.1f ⭐", doubleVal(data, "averageRating"))),
                    makeStatLine("Reactions", String.valueOf(intVal(data, "reactionCounts")))
            );
            row2.getChildren().addAll(attendancePie, summaryCard);
        }

        // Row 3: summary stats bar
        summaryBar.getChildren().addAll(
                makeSmallCard("Total Booked", String.valueOf(intVal(data, "totalBooked"))),
                makeSmallCard("Attendance Rate", intVal(data, "attendancePercent") + "%"),
                makeSmallCard("Revenue", "KES " + intVal(data, "totalRevenue")),
                makeSmallCard("Avg Rating", String.format("%.1f ⭐", doubleVal(data, "averageRating")))
        );

        // Export button
        Button exportBtn = new Button("📥 Export CSV");
        exportBtn.getStyleClass().add("btn-secondary");
        exportBtn.setOnAction(e -> onExport(eventId));

        chartsContainer.getChildren().addAll(row1, row2, exportBtn);
    }

    private void onExport(Long eventId) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save CSV");
        chooser.setInitialFileName("tickets_" + eventId + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showSaveDialog(chartsContainer.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String baseUrl = ApiClient.getInstance().getBaseUrl();
                String token = SessionManager.getInstance().getJwtToken();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/tickets/export/" + eventId))
                        .header("Authorization", "Bearer " + token)
                        .GET().build();
                HttpResponse<byte[]> resp = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
                Files.write(file.toPath(), resp.body());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "CSV exported to " + file.getName());
            alert.showAndWait();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed.");
            alert.showAndWait();
        }));
        runBackground(task);
    }

    // ── helpers ──

    private VBox makeSmallCard(String label, String value) {
        VBox card = new VBox(2);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        v.setStyle("-fx-font-size: 18px;");
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        card.getChildren().addAll(v, l);
        return card;
    }

    private HBox makeStatLine(String label, String value) {
        HBox line = new HBox(8);
        Label l = new Label(label + ":");
        l.setStyle("-fx-font-weight: bold;");
        Label v = new Label(value);
        line.getChildren().addAll(l, v);
        return line;
    }

    private String str(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? v.toString() : ""; }
    private int intVal(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof Number ? ((Number)v).intValue() : 0; }
    private double doubleVal(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof Number ? ((Number)v).doubleValue() : 0.0; }
    private void runBackground(Task<?> t) { Thread th = new Thread(t); th.setDaemon(true); th.start(); }
}
