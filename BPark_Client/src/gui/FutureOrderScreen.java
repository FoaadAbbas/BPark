package gui;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class represents the FutureOrderScreen.
 */
public class FutureOrderScreen {
    private Stage primaryStage;
    private ClientUi clientUi; // Reference back to the main UI controller

    public FutureOrderScreen(Stage primaryStage, ClientUi clientUi) {
        this.primaryStage = primaryStage;
        this.clientUi = clientUi;
    }

    /**
     * show method.
     *
     * 
     */
    public void show() {
        // --- Center Content ---
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(600); // Increased width

        Label titleLabel = new Label("Order Future Parking");
        titleLabel.getStyleClass().add("title-label");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        Label dateLabel = new Label("Date:");
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        grid.add(dateLabel, 0, 0);
        grid.add(datePicker, 1, 0);

        Label hourLabel = new Label("Hour (0-23):");
        ComboBox<Integer> hourSelector = new ComboBox<>(
                FXCollections.observableArrayList(IntStream.range(0, 24).boxed().collect(Collectors.toList()))
        );
        hourSelector.setValue(12);
        grid.add(hourLabel, 0, 1);
        grid.add(hourSelector, 1, 1);

        Label minuteLabel = new Label("Minute:");
        ComboBox<Integer> minuteSelector = new ComboBox<>(
                FXCollections.observableArrayList(0, 15, 30, 45)
        );
        minuteSelector.setValue(0);
        grid.add(minuteLabel, 0, 2);
        grid.add(minuteSelector, 1, 2);

        Button confirmButton = new Button("Confirm Reservation");
        confirmButton.setMaxWidth(Double.MAX_VALUE);

        confirmButton.setOnAction(e -> {
            LocalDate date = datePicker.getValue();
            Integer hour = hourSelector.getValue();
            Integer minute = minuteSelector.getValue();

            if (date == null || hour == null || minute == null) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please select a valid date, hour, and minute.");
                return;
            }

            LocalDateTime selectedDateTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
            if (selectedDateTime.isBefore(LocalDateTime.now())) {
                showAlert(Alert.AlertType.ERROR, "Invalid Date/Time", "Cannot book parking for a date or time that has already passed.");
                return;
            }

            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String timeStr = String.format("%02d:%02d", hour, minute);

            String msg = "FUTURE_PARK_REQUEST;" + dateStr + ";" + timeStr;
            try {
                clientUi.getClient().sendToServer(msg);
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Connection Error", "Failed to send reservation request: " + ex.getMessage());
            }
        });

        content.getChildren().addAll(titleLabel, grid, confirmButton);

        // --- Left Navigation ---
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Dashboard");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> clientUi.showClientDashboard());
        leftNav.getChildren().add(backButton);

        // --- Create Scene with Standard Layout ---
        Scene scene = clientUi.createMainViewScene("Order Future Parking", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Future Reservation");
        primaryStage.setMaximized(true);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}