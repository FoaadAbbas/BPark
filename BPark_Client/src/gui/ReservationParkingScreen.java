package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * This class represents the ReservationParkingScreen.
 */
public class ReservationParkingScreen {
    private Stage primaryStage;
    private ClientUi.MyClient client;
    private ClientUi clientUi; // Added for standard layout

    // MODIFIED: Constructor now takes ClientUi instance for standard layout creation
    public ReservationParkingScreen(Stage primaryStage, ClientUi.MyClient client, ClientUi clientUi) {
        this.primaryStage = primaryStage;
        this.client = client;
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
        content.setMaxWidth(600); // Increased width for better space utilization
        content.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Park with Reservation");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 15, 0));

        Label codeLabel = new Label("Enter Your Reservation Code:");
        TextField codeField = new TextField();
        codeField.setPromptText("e.g., ABC123XYZ");

        Button parkButton = new Button("Confirm Parking");
        parkButton.setMaxWidth(Double.MAX_VALUE);
        parkButton.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                showAlert("Reservation code cannot be empty.", Alert.AlertType.WARNING);
                return;
            }

            try {
                client.sendToServer("PARK_WITH_RESERVATION;" + code);
            } catch (IOException ex) {
                showAlert("Failed to send request to server: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        HBox buttonBox = new HBox(10, parkButton);
        VBox.setMargin(buttonBox, new Insets(10, 0, 0, 0));

        content.getChildren().addAll(
            titleLabel,
            codeLabel,
            codeField,
            buttonBox
        );

        // --- Left Navigation ---
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Parking Options");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        // Navigate back to the ParkingActionScreen
        backButton.setOnAction(e -> new ParkingActionScreen(primaryStage, client, clientUi).show());
        leftNav.getChildren().add(backButton);

        // --- Create Scene with Standard Layout ---
        Scene scene = clientUi.createMainViewScene("Park with Reservation", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Park with Reservation");
        primaryStage.setMaximized(true);
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}