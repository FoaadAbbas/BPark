package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ocsf.client.AbstractClient;
import java.io.IOException;

public class VehicleReleaseScreen {
    private Stage primaryStage;
    private AbstractClient client;
    private ClientUi clientUi;

    // MODIFIED: Constructor now takes ClientUi instance for standard layout creation
    public VehicleReleaseScreen(Stage primaryStage, AbstractClient client, ClientUi clientUi) {
        this.primaryStage = primaryStage;
        this.client = client;
        this.clientUi = clientUi;
    }

    public void show() {
        // --- Center Content ---
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(600); // Increased width
        content.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Release Your Vehicle");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 15, 0));

        Label codeLabel = new Label("Enter Your Confirmation Code:");
        TextField codeField = new TextField();
        codeField.setPromptText("e.g., ABC123");

        Button releaseButton = new Button("Release Vehicle");
        releaseButton.setMaxWidth(Double.MAX_VALUE);
        releaseButton.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                showAlert("Confirmation code cannot be empty.", Alert.AlertType.WARNING);
                return;
            }
            
            try {
                client.sendToServer("RELEASE_VEHICLE;" + code);
            } catch (IOException ex) {
                showAlert("Failed to send request to server: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        Button forgotCodeButton = new Button("Forgot my confirmation code?");
        forgotCodeButton.getStyleClass().add("link-button"); 
        forgotCodeButton.setOnAction(e -> {
            try {
                client.sendToServer("FORGOT_CONFIRMATION_CODE");
            } catch (IOException ex) {
                showAlert("Failed to send request to server: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        HBox buttonBox = new HBox(10, releaseButton);
        VBox.setMargin(buttonBox, new Insets(10, 0, 0, 0));

        content.getChildren().addAll(
            titleLabel, 
            codeLabel, 
            codeField, 
            buttonBox, 
            forgotCodeButton
        );

        // --- Left Navigation ---
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Dashboard");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> clientUi.showClientDashboard());
        leftNav.getChildren().add(backButton);

        // --- Create Scene with Standard Layout ---
        Scene scene = clientUi.createMainViewScene("Take My Car", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Vehicle Release");
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