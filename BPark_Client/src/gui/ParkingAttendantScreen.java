package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.UUID;

/**
 * This class represents the ParkingAttendantScreen.
 */
public class ParkingAttendantScreen {
    private Stage primaryStage;
    private ClientUi clientUi;

    public ParkingAttendantScreen(Stage primaryStage, ClientUi clientUi) {
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
        content.setMaxWidth(700); // Increased width for the form

        // --- Title ---
        Label titleLabel = new Label("Parking Attendant Dashboard");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 15, 0));

        // --- Registration Part ---
        Label registrationLabel = new Label("New Subscriber Registration");
        registrationLabel.getStyleClass().add("header-label");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField userNameField = new TextField();
        grid.add(new Label("User Name:"), 0, 0);
        grid.add(userNameField, 1, 0);

        TextField phoneField = new TextField();
        grid.add(new Label("Phone Number:"), 0, 1);
        grid.add(phoneField, 1, 1);

        TextField emailField = new TextField();
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);

        TextField idField = new TextField();
        grid.add(new Label("ID:"), 0, 3);
        grid.add(idField, 1, 3);

        String generatedCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TextField codeField = new TextField(generatedCode);
        codeField.setEditable(false);
        grid.add(new Label("Subscription Code:"), 0, 4);
        grid.add(codeField, 1, 4);

        Button registerButton = new Button("Register Subscriber");
        registerButton.setMaxWidth(Double.MAX_VALUE);

        registerButton.setOnAction(e -> {
            String userName = userNameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String id = idField.getText().trim();
            String code = codeField.getText();

            if (userName.isEmpty() || phone.isEmpty() || email.isEmpty() || id.isEmpty()) {
                showAlert("All fields must be filled.", Alert.AlertType.WARNING);
                return;
            }

            String msg = String.format("REGISTER_SUBSCRIBER;%s;%s;%s;%s;%s",
                    code, userName, phone, email, id);
            try {
                clientUi.getClient().sendToServer(msg);
                showAlert("Subscriber Registered Successfully.", Alert.AlertType.INFORMATION);
                // Clear fields for next registration
                userNameField.clear();
                phoneField.clear();
                emailField.clear();
                idField.clear();
                codeField.setText(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            } catch (Exception ex) {
                showAlert("Failed to send data to server: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        HBox registrationButtonBox = new HBox(10, registerButton);
        registrationButtonBox.setAlignment(Pos.CENTER_RIGHT);
        
        // --- View Data Part ---
        Separator separator = new Separator();
        VBox.setMargin(separator, new Insets(20, 0, 20, 0));

        Label viewLabel = new Label("View System Data");
        viewLabel.getStyleClass().add("header-label");

        Button viewParkingButton = new Button("View Active Parking");
        viewParkingButton.setMaxWidth(Double.MAX_VALUE);
        viewParkingButton.setOnAction(e -> {
            try {
                clientUi.expectingManagerDataType = "orders";
                clientUi.getClient().tableContext = "orders";
                clientUi.getClient().sendToServer("get_all_orders");
            } catch (IOException ex) {
                showAlert("Failed to request parking data: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        Button viewSubscribersButton = new Button("View Subscribers");
        viewSubscribersButton.setMaxWidth(Double.MAX_VALUE);
        viewSubscribersButton.setOnAction(e -> {
            try {
                clientUi.expectingManagerDataType = "subscribers";
                clientUi.getClient().tableContext = "subscribers";
                clientUi.getClient().sendToServer("get_all_subscribers");
            } catch (IOException ex) {
                showAlert("Failed to request subscriber data: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        VBox viewButtons = new VBox(10, viewParkingButton, viewSubscribersButton);

        // --- Assembling the View ---
        content.getChildren().addAll(
                titleLabel,
                registrationLabel,
                grid,
                registrationButtonBox,
                separator,
                viewLabel,
                viewButtons
        );
        
        // --- Left Navigation ---
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Roles");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> clientUi.showRoleSelectionScreen());
        leftNav.getChildren().add(backButton);

        // --- Create Scene with Standard Layout ---
        Scene scene = clientUi.createMainViewScene("Parking Attendant Dashboard", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Parking Attendant");
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