package gui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * This class represents the ParkingActionScreen.
 */
public class ParkingActionScreen {
    private Stage primaryStage;
    private ClientUi clientUi;
    private ClientUi.MyClient client;

    public ParkingActionScreen(Stage primaryStage, ClientUi.MyClient client, ClientUi clientUi) {
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
        VBox content = new VBox(20);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(600); // Increased width
        content.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Parking Options");
        titleLabel.getStyleClass().add("title-label");

        Button parkNewCarBtn = new Button("Park a New Car");
        parkNewCarBtn.setMaxWidth(Double.MAX_VALUE);
        parkNewCarBtn.setOnAction(e -> {
            client.handleMessageFromClientUI("PARK_REQUEST");
        });

        Button extendParkingBtn = new Button("Extend My Parking Session");
        extendParkingBtn.setMaxWidth(Double.MAX_VALUE);
        extendParkingBtn.setOnAction(e -> {
            new ExtensionScreen(primaryStage, client, clientUi).show();
        });

        Button parkWithReservationBtn = new Button("Park with a Reservation");
        parkWithReservationBtn.setMaxWidth(Double.MAX_VALUE);
        parkWithReservationBtn.setOnAction(e -> {
            // Pass the clientUi instance to the constructor
            new ReservationParkingScreen(primaryStage, client, clientUi).show();
        });

        content.getChildren().addAll(
                titleLabel,
                parkNewCarBtn,
                extendParkingBtn,
                parkWithReservationBtn
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
        Scene scene = clientUi.createMainViewScene("Park or Extend", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Parking Actions");
        primaryStage.setMaximized(true);
    }
}