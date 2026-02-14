package gui;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.stream.IntStream;

import gui.ClientUi.MyClient;

import java.util.stream.Collectors;

public class ExtensionScreen {
    private Stage primaryStage;
    private MyClient client;
    private ClientUi clientUi;

    public ExtensionScreen(Stage primaryStage, MyClient client, ClientUi clientUi) {
        this.primaryStage = primaryStage;
        this.client = client;
        this.clientUi = clientUi;
    }

    public void show() {
        // --- Center Content ---
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(600); // Increased width for better space utilization

        Label titleLabel = new Label("Extend Parking Time");
        titleLabel.getStyleClass().add("title-label");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        // Hour Selector
        Label hourLabel = new Label("Hours to Add (Max 4):");
        ComboBox<Integer> hourSelector = new ComboBox<>(
                FXCollections.observableArrayList(IntStream.rangeClosed(1, 4).boxed().collect(Collectors.toList()))
        );
        hourSelector.setValue(1); // Default to 1 hour
        grid.add(hourLabel, 0, 0);
        grid.add(hourSelector, 1, 0);

        Button confirmButton = new Button("Confirm Extension");
        confirmButton.setMaxWidth(Double.MAX_VALUE);

        confirmButton.setOnAction(e -> {
            Integer hours = hourSelector.getValue();
            if (hours == null) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please select the number of hours to add.");
                return;
            }

            String msg = "EXTEND_PARKING;" + hours;
            try {
                client.sendToServer(msg);
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Connection Error", "Failed to send extension request: " + ex.getMessage());
            }
        });

        content.getChildren().addAll(titleLabel, grid, confirmButton);
        
        // --- Left Navigation ---
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Parking Options");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> new ParkingActionScreen(primaryStage, client, clientUi).show());
        leftNav.getChildren().add(backButton);

        // --- Create Scene with Standard Layout ---
        Scene scene = clientUi.createMainViewScene("Extend Parking", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Extend Parking");
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