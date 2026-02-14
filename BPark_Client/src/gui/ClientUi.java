package gui;

import common.ActivityInfo;
import common.DailyLateData;
import common.MonthlyReportData;
import common.OrderInfo;
import common.SlotOccupancyData;
import common.SubscriberInfo;
import common.SubscriberParkingData;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import ocsf.client.AbstractClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class represents the ClientUi.
 */
public class ClientUi extends Application {
    private String role;
    private String requestedFutureDate;
    /**
     * getClient method.
     *
     * @return result
     */
    public MyClient getClient() {
        return client;
    }
    /**
     * showClientDashboard method.
     *
     * 
     */
    public void showClientDashboard() {
        // The check 'if (dashboardShown) return;' was causing the issue.
        // It's better to allow re-rendering the dashboard.
        // if (dashboardShown) return; // REMOVED FOR BUG FIX
        
        dashboardShown = true;

        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("main-layout");

        // --- TOP HEADER ---
        Label titleLabel = new Label("BPark Client Dashboard");
        titleLabel.getStyleClass().add("title-label");
        HBox topBar = new HBox(titleLabel);
        topBar.getStyleClass().add("header-pane");
        topBar.setAlignment(Pos.CENTER_LEFT);
        mainLayout.setTop(topBar);

        if (subscriberInfo != null) {
            Label welcomeLabel = new Label("Welcome, " + subscriberInfo.getUserName() + "!");
            welcomeLabel.getStyleClass().add("header-label");
            Label separatorLabel = new Label("  |  ");
            separatorLabel.setStyle("-fx-text-fill: #555;");
            topBar.getChildren().addAll(separatorLabel, welcomeLabel);
        }

        // --- LEFT NAVIGATION ---
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        leftNav.setAlignment(Pos.TOP_LEFT);

        Button parkCarBtn = new Button("Park or Extend");
        parkCarBtn.setMaxWidth(Double.MAX_VALUE);
        parkCarBtn.getStyleClass().add("nav-button");
        parkCarBtn.setOnAction(e -> {
            dashboardShown = false; // FIX: Reset flag before navigating away
            new ParkingActionScreen(primaryStage, client, this).show();
        });

        Button takeCarBtn = new Button("Take My Car");
        takeCarBtn.setMaxWidth(Double.MAX_VALUE);
        takeCarBtn.getStyleClass().add("nav-button");
        takeCarBtn.setOnAction(e -> {
            dashboardShown = false; // FIX: Reset flag before navigating away
            new VehicleReleaseScreen(primaryStage, client, this).show();
        });

        Button historyBtn = new Button("See My History");
        historyBtn.setMaxWidth(Double.MAX_VALUE);
        historyBtn.getStyleClass().add("nav-button");
        historyBtn.setOnAction(e -> {
            try {
                expectingHistory = true;
                client.sendToServer("GET_HISTORY;" + subscriberInfo.getSubscriptionCode());
            } catch (IOException ex) {
                showError("Failed to request history data from the server.");
            }
        });

        Button orderFutureBtn = new Button("Order Future Parking");
        orderFutureBtn.setMaxWidth(Double.MAX_VALUE);
        orderFutureBtn.getStyleClass().add("nav-button");
        orderFutureBtn.setOnAction(e -> {
            dashboardShown = false; // FIX: Reset flag before navigating away
            FutureOrderScreen futureOrderScreen = new FutureOrderScreen(primaryStage, this);
            futureOrderScreen.show();
        });

        Button updateDataBtn = new Button("Update My Data");
        updateDataBtn.setMaxWidth(Double.MAX_VALUE);
        updateDataBtn.getStyleClass().add("nav-button");
        updateDataBtn.setOnAction(e -> {
            showUpdateDataScreen();
        });

        Pane spacer = new Pane();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().addAll("nav-button", "secondary-button");
        logoutButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setOnAction(e -> showRoleSelectionScreen());

        leftNav.getChildren().addAll(parkCarBtn, takeCarBtn, historyBtn, orderFutureBtn, updateDataBtn, spacer, logoutButton);
        mainLayout.setLeft(leftNav);

        // --- CENTER CONTENT ---
        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.getStyleClass().add("main-content-area");
        Label loadingLabel = new Label("Loading Parking Slots...");
        loadingLabel.getStyleClass().add("header-label");
        centerContent.getChildren().add(loadingLabel);
        mainLayout.setCenter(centerContent);

        try {
            expectingSlots = true;
            client.sendToServer("show_slots");
        } catch (IOException e) {
            showError("Failed to request current slot data from server: " + e.getMessage());
            expectingSlots = false;
        }

        Scene scene = new Scene(mainLayout);
        scene.getStylesheets().add(getClass().getResource("modern-theme.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Dashboard");
        primaryStage.setMaximized(true);
    }
    private void updateClientDashboardSlots(ArrayList<Integer> occupiedSlots) {
        if (primaryStage.getScene().getRoot() instanceof BorderPane mainLayout) {
            SlotViewer viewer = new SlotViewer(occupiedSlots);
            GridPane grid = viewer.getGrid();
            Label title = new Label("Live Parking Lot Status");
            title.getStyleClass().add("header-label");
            VBox centerContent = new VBox(20, title, grid);
            centerContent.setAlignment(Pos.CENTER);
            centerContent.getStyleClass().add("main-content-area");
            mainLayout.setCenter(centerContent);
        }
    }

    public ClientUi() {}

    public ClientUi(String role) {
        this.role = role;
    }
    private boolean expectingSlots = false;
    public String expectingManagerDataType = null;
    private boolean expectingHistory = false;
    private boolean dashboardShown = false;

    private Label resultLabel;
    private Stage primaryStage;
    private MyClient client;
    private TextField ipField;
    private TextField portField;
    private Label statusLabel;
    private SubscriberInfo subscriberInfo;

    @Override
    /**
     * start method.
     *
     * @param primaryStage the primaryStage
     * 
     */
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("BPark Client");
        primaryStage.setWidth(1536);
        primaryStage.setHeight(864);
        primaryStage.setOnCloseRequest(e -> {
            quit();
            Platform.exit();
            System.exit(0);
        });
        resultLabel = new Label();
        showConnectionScreen();
    }

    private Scene createStyledScene(Region layout) {
        StackPane root = new StackPane(layout);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("modern-theme.css").toExternalForm());
        return scene;
    }

    /**
     * showRoleSelectionScreen method.
     *
     * 
     */
    public void showRoleSelectionScreen() {
        dashboardShown = false;
        this.expectingManagerDataType = null;
        this.expectingHistory = false;
        this.subscriberInfo = null;
        this.role = null;

        VBox content = new VBox(20);
        content.setMaxWidth(400);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("content-pane");

        Label titleLabel = new Label("Choose Your Role");
        titleLabel.getStyleClass().add("title-label");

        Button clientBtn = new Button("Client");
        clientBtn.setMaxWidth(Double.MAX_VALUE);
        clientBtn.setOnAction(e -> handleRoleSelection("CLIENT"));

        Button managerBtn = new Button("Manager");
        managerBtn.setMaxWidth(Double.MAX_VALUE);
        managerBtn.setOnAction(e -> handleRoleSelection("MANAGER"));

        Button attendantBtn = new Button("Parking Attendant");
        attendantBtn.setMaxWidth(Double.MAX_VALUE);
        attendantBtn.setOnAction(e -> handleRoleSelection("PARKING_ATTENDANT"));

        Button viewSlotsBtn = new Button("View Parking Slots");
        viewSlotsBtn.setMaxWidth(Double.MAX_VALUE);
        viewSlotsBtn.setOnAction(e -> handleRoleSelection("VIEW_SLOTS"));

        Button disconnectBtn = new Button("Disconnect");
        disconnectBtn.setMaxWidth(Double.MAX_VALUE);
        disconnectBtn.getStyleClass().add("secondary-button");
        disconnectBtn.setOnAction(e -> {
            quit();
            showConnectionScreen();
        });

        content.getChildren().addAll(titleLabel, clientBtn, managerBtn, attendantBtn, viewSlotsBtn, new Separator(), disconnectBtn);

        Scene roleScene = createStyledScene(content);
        primaryStage.setTitle("BPark - Role Selection");
        primaryStage.setScene(roleScene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void handleRoleSelection(String selectedRole) {
        this.role = selectedRole;
        switch (role) {
            case "CLIENT":
                showLoginScreen();
                break;
            case "VIEW_SLOTS":
                try {
                    expectingSlots = true;
                    client.sendToServer("show_slots");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    expectingSlots = false;
                    showError("Failed to request slot data.");
                }
                break;
            case "MANAGER":
                showAdminLoginScreen(); // Redirect to Admin Login
                break;
            case "PARKING_ATTENDANT":
                showAttendantLoginScreen(); // Redirect to Attendant Login
                break;
        }
    }

    private void showAdminLoginScreen() {
        dashboardShown = false;
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(450);
        content.getStyleClass().add("content-pane");

        Label titleLabel = new Label("Admin Login");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter admin username");

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter admin password");

        Button loginBtn = new Button("Login as Admin");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.getStyleClass().add("result-label");

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if ("foaad".equals(username) && "ADMINBPARK".equals(password)) {
                resultLabel.setText("Admin login successful!");
                resultLabel.getStyleClass().setAll("result-label");
                // Navigate to Manager/Admin Dashboard
                try {
                    expectingManagerDataType = "scheduled_orders";
                    client.tableContext = "manager";
                    client.sendToServer("get_all_scheduled_orders");
                } catch (IOException ex) {
                    expectingManagerDataType = null;
                    showError("Failed to request manager data from server.");
                }
            } else {
                resultLabel.setText("Invalid admin credentials.");
                resultLabel.getStyleClass().setAll("result-label", "result-label-error");
            }
        });

        Button backButton = new Button("Back to Roles");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.getStyleClass().add("secondary-button");
        backButton.setOnAction(e -> showRoleSelectionScreen());

        content.getChildren().addAll(
                titleLabel, usernameLabel, usernameField, passwordLabel, passwordField, loginBtn, resultLabel,
                new Separator(), backButton
        );

        Scene loginScene = createStyledScene(content);
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("BPark - Admin Login");
        primaryStage.setMaximized(true);
    }

    private void showAttendantLoginScreen() {
        dashboardShown = false;
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(450);
        content.getStyleClass().add("content-pane");

        Label titleLabel = new Label("Parking Attendant Login");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        Label keyLabel = new Label("Attendant Key:");
        PasswordField keyField = new PasswordField();
        keyField.setPromptText("Enter attendant key");

        Button loginBtn = new Button("Login as Attendant");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.getStyleClass().add("result-label");

        loginBtn.setOnAction(e -> {
            String key = keyField.getText().trim();

            if ("bParkG4".equals(key)) {
                resultLabel.setText("Attendant login successful!");
                resultLabel.getStyleClass().setAll("result-label");
                // Navigate to Parking Attendant Dashboard
                new ParkingAttendantScreen(primaryStage, this).show();
            } else {
                resultLabel.setText("Invalid attendant key.");
                resultLabel.getStyleClass().setAll("result-label", "result-label-error");
            }
        });

        Button backButton = new Button("Back to Roles");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.getStyleClass().add("secondary-button");
        backButton.setOnAction(e -> showRoleSelectionScreen());

        content.getChildren().addAll(
                titleLabel, keyLabel, keyField, loginBtn, resultLabel,
                new Separator(), backButton
        );

        Scene loginScene = createStyledScene(content);
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("BPark - Attendant Login");
        primaryStage.setMaximized(true);
    }
	private void showConnectionScreen() {
        dashboardShown = false;
        VBox content = new VBox(15);
        content.setMaxWidth(400);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("content-pane");

        Label titleLabel = new Label("BPark Server Connection");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        Label ipLabel = new Label("Server IP:");
        ipField = new TextField("127.0.0.1");

        Label portLabel = new Label("Port:");
        portField = new TextField("5555");

        Button connectButton = new Button("Connect");
        connectButton.setMaxWidth(Double.MAX_VALUE);
        connectButton.setOnAction(e -> connectToServer());

        VBox.setMargin(connectButton, new Insets(10, 0, 0, 0));

        content.getChildren().addAll(titleLabel, ipLabel, ipField, portLabel, portField, connectButton);

        Scene scene = createStyledScene(content);
        primaryStage.setTitle("BPark Client - Connect");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }
    
    private void quit() {
        try {
            if (client != null && client.isConnected()) {
                client.sendToServer("CLIENT_DISCONNECTED");
                Thread.sleep(100);
                client.closeConnection();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    private void showLoginScreen() {
        dashboardShown = false;
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(450);
        content.getStyleClass().add("content-pane");

        Label titleLabel = new Label("Client Login");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        Label codeLabel = new Label("Subscription Code:");
        TextField codeField = new TextField();
        codeField.setPromptText("Enter your subscription code");
        Button loginBtn = new Button("Login with Subscription");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.getStyleClass().add("result-label");

        loginBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                resultLabel.setText("Please enter a subscription code.");
                resultLabel.getStyleClass().setAll("result-label", "result-label-error");
                return;
            }
            try {
                client.sendToServer("LOGIN:" + code);
                resultLabel.setText("Authenticating...");
                resultLabel.getStyleClass().setAll("result-label");
            } catch (IOException ex) {
                ex.printStackTrace();
                resultLabel.setText("Failed to send login data to server.");
                resultLabel.getStyleClass().setAll("result-label", "result-label-error");
            }
        });

        Separator orSeparator = new Separator();
        Label orLabel = new Label("OR");
        StackPane separatorWithText = new StackPane(orSeparator, orLabel);
        orLabel.setStyle("-fx-background-color: #2d2d30; -fx-padding: 0 10 0 10;");
        VBox.setMargin(separatorWithText, new Insets(10, 0, 10, 0));

        Button rtaTagLoginBtn = new Button("RTA Tag Login");
        rtaTagLoginBtn.setMaxWidth(Double.MAX_VALUE);
        rtaTagLoginBtn.setOnAction(e -> showRtaTagDemoScreen());

        Button backButton = new Button("Back to Roles");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.getStyleClass().add("secondary-button");
        backButton.setOnAction(e -> showRoleSelectionScreen());

        content.getChildren().addAll(
                titleLabel, codeLabel, codeField, loginBtn, resultLabel,
                separatorWithText, rtaTagLoginBtn, new Separator(), backButton
        );

        Scene loginScene = createStyledScene(content);
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("BPark - Client Login");
        primaryStage.setMaximized(true);
    }

    private void showRtaTagDemoScreen() {
        dashboardShown = false;
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(500);
        content.getStyleClass().add("content-pane");

        Label titleLabel = new Label("RTA Tag Scan");
        titleLabel.getStyleClass().add("title-label");

        Label instructionLabel = new Label("Place your tag on the sensor as shown below.");
        instructionLabel.getStyleClass().add("header-label");

        Pane animationPane = new Pane();
        animationPane.setPrefSize(250, 150);

        Rectangle sensorShape = new Rectangle(80, 80, Color.web("#444"));
        sensorShape.setArcWidth(15);
        sensorShape.setArcHeight(15);
        Text sensorText = new Text("SENSOR");
        sensorText.setFont(Font.font("Segoe UI", 12));
        sensorText.setFill(Color.LIGHTGRAY);
        StackPane sensor = new StackPane(sensorShape, sensorText);
        sensor.setLayoutX(150);
        sensor.setLayoutY(35);

        Rectangle tag = new Rectangle(50, 70, Color.web("#007ACC"));
        tag.setArcWidth(10);
        tag.setArcHeight(10);
        tag.setLayoutX(35);
        tag.setLayoutY(40);
        tag.setOpacity(0);

        animationPane.getChildren().addAll(sensor, tag);

        TranslateTransition moveTag = new TranslateTransition(Duration.seconds(1.2), tag);
        moveTag.setToX(125);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), tag);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        PauseTransition hold = new PauseTransition(Duration.seconds(1.0));
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), tag);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        SequentialTransition sequence = new SequentialTransition(fadeIn, moveTag, hold, fadeOut);
        sequence.setCycleCount(Animation.INDEFINITE);
        sequence.play();

        Label statusLabel = new Label("Scanning for tag...");
        statusLabel.getStyleClass().add("result-label");

        VBox.setMargin(titleLabel, new Insets(0, 0, 10, 0));
        VBox.setMargin(instructionLabel, new Insets(0, 0, 15, 0));
        VBox.setMargin(statusLabel, new Insets(15, 0, 0, 0));
        content.getChildren().addAll(titleLabel, instructionLabel, animationPane, statusLabel);

        Scene scene = createStyledScene(content);
        primaryStage.setScene(scene);
        primaryStage.setTitle("RTA Tag Login");
        primaryStage.setMaximized(true);

        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(event -> {
            sequence.stop();
            SubscriberInfo demoSubscriber = new SubscriberInfo("RTA-TAG-CODE", "RTA Tag User", "000-000-0000", "rta@example.com", "RTA-12345", 0, false);
            this.subscriberInfo = demoSubscriber;
            Platform.runLater(this::showClientDashboard);
        });
        delay.play();
    }
    
    private void showUpdateDataScreen() {
        dashboardShown = false;

        VBox contentPane = new VBox(15);
        contentPane.getStyleClass().add("content-pane");
        contentPane.setMaxWidth(450);

        Label titleLabel = new Label("Update My Information");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 15, 0));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField userNameField = new TextField(subscriberInfo.getUserName());
        grid.add(new Label("User Name:"), 0, 0);
        grid.add(userNameField, 1, 0);

        TextField phoneField = new TextField(subscriberInfo.getPhoneNumber());
        grid.add(new Label("Phone Number:"), 0, 1);
        grid.add(phoneField, 1, 1);

        TextField emailField = new TextField(subscriberInfo.getEmail());
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);

        Label idLabel = new Label(subscriberInfo.getId());
        grid.add(new Label("ID:"), 0, 3);
        grid.add(idLabel, 1, 3);

        Label codeLabel = new Label(subscriberInfo.getSubscriptionCode());
        grid.add(new Label("Subscription Code:"), 0, 4);
        grid.add(codeLabel, 1, 4);

        Button updateButton = new Button("Save Changes");
        updateButton.setMaxWidth(Double.MAX_VALUE);

        updateButton.setOnAction(e -> {
            String newUserName = userNameField.getText().trim();
            String newPhone = phoneField.getText().trim();
            String newEmail = emailField.getText().trim();

            if (newUserName.isEmpty() || newPhone.isEmpty() || newEmail.isEmpty()) {
                showError("User Name, Phone, and Email cannot be empty.");
                return;
            }

            String msg = String.format("UPDATE_SUBSCRIBER_INFO;%s;%s;%s;%s",
                    subscriberInfo.getSubscriptionCode(), newUserName, newPhone, newEmail);
            try {
                client.sendToServer(msg);
            } catch (IOException ex) {
                showError("Failed to send update request: " + ex.getMessage());
            }
        });

        contentPane.getChildren().addAll(titleLabel, grid, updateButton);

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Dashboard");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> showClientDashboard());
        leftNav.getChildren().add(backButton);
        
        Scene scene = createMainViewScene("Update Information", contentPane, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Update Information");
        primaryStage.setMaximized(true);
    }

    private void connectToServer() {
        String ip = ipField.getText();
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            showError("Port must be a valid number");
            return;
        }

        client = new MyClient(ip, port);
        try {
            showClientConsole("Connecting to server...");
            client.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to connect to server: " + e.getMessage());
            showConnectionScreen();
        }
    }
    
    private void showClientConsole(String status) {
        dashboardShown = false;
        VBox content = new VBox(20);
        content.setMaxWidth(400);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("content-pane");

        Label titleLabel = new Label("BPark Client");
        titleLabel.getStyleClass().add("title-label");

        statusLabel = new Label(status);
        ProgressIndicator progress = new ProgressIndicator();
        content.getChildren().addAll(titleLabel, statusLabel, progress);

        Scene scene = createStyledScene(content);
        primaryStage.setTitle("Client Console");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }
    
	/**
	* MODIFIED: Changed from private to package-private to allow other UI screens to use it.
	*/
    /**
     * createMainViewScene method.
     *
     * @param title the title
     * @param mainContent the mainContent
     * @param leftNav the leftNav
     * @return result
     */
    public Scene createMainViewScene(String title, Node mainContent, Node leftNav) {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("main-layout");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-label");
        HBox topBar = new HBox(titleLabel);
        topBar.getStyleClass().add("header-pane");
        layout.setTop(topBar);

        if (leftNav != null) {
            layout.setLeft(leftNav);
        }

        StackPane centerWrapper = new StackPane(mainContent);
        centerWrapper.getStyleClass().add("main-content-area");
        StackPane.setAlignment(mainContent, Pos.TOP_CENTER);
        layout.setCenter(centerWrapper);

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(getClass().getResource("modern-theme.css").toExternalForm());
        return scene;
    }

    private void showHistoryPage(ArrayList<ActivityInfo> history) {
        dashboardShown = false;
        
        TableView<ActivityInfo> table = new TableView<>();
        TableColumn<ActivityInfo, String> activityCol = new TableColumn<>("Activity");
        activityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActivityType()));
        activityCol.setStyle("-fx-alignment: CENTER;");
        TableColumn<ActivityInfo, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDetails()));
        detailsCol.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<ActivityInfo, String> timestampCol = new TableColumn<>("Date & Time");
        timestampCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp()));
        timestampCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().addAll(activityCol, detailsCol, timestampCol);
        if (history != null) {
            table.getItems().addAll(history);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No activity history found."));

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Dashboard");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> showClientDashboard());
        leftNav.getChildren().add(backButton);
        
        Scene scene = createMainViewScene("My Activity History", table, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Activity History");
        primaryStage.setMaximized(true);
    }

    private void showAllActivityHistoryPage(ArrayList<ActivityInfo> activities) {
        dashboardShown = false;

        TableView<ActivityInfo> table = new TableView<>();
        TableColumn<ActivityInfo, String> subCodeCol = new TableColumn<>("Subscriber Code");
        subCodeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubscriberCode()));
        TableColumn<ActivityInfo, String> userNameCol = new TableColumn<>("User Name");
        userNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserName()));
        TableColumn<ActivityInfo, String> activityCol = new TableColumn<>("Activity Type");
        activityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActivityType()));
        TableColumn<ActivityInfo, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDetails()));
        TableColumn<ActivityInfo, String> timestampCol = new TableColumn<>("Date & Time");
        timestampCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp()));
        table.getColumns().addAll(subCodeCol, userNameCol, activityCol, detailsCol, timestampCol);
        if (activities != null) {
            table.getItems().addAll(activities);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No activity has been logged in the system."));
        table.getColumns().forEach(col -> col.setStyle("-fx-alignment: CENTER;"));

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        Scene scene = createMainViewScene("System-Wide Activity Log", table, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - System Activity Log");
        primaryStage.setMaximized(true);
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * main method.
     *
     * @param args the args
     * 
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    private void showSlotsAndGoBack(ArrayList<Integer> occupiedSlots) {
        dashboardShown = false;
        SlotViewer viewer = new SlotViewer(occupiedSlots);
        GridPane grid = viewer.getGrid();
        
        VBox content = new VBox(20, grid);
        content.setAlignment(Pos.CENTER);

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Roles");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> showRoleSelectionScreen());
        leftNav.getChildren().add(backButton);

        Scene scene = createMainViewScene("Parking Slot Viewer", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }
    
    private void showReportSelectionScreen() {
        dashboardShown = false;
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(400);
        content.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Generate Monthly Report");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 15, 0));

        Label yearLabel = new Label("Select Year:");
        int currentYear = LocalDate.now().getYear();
        ComboBox<Integer> yearSelector = new ComboBox<>(
            FXCollections.observableArrayList(
                IntStream.rangeClosed(currentYear - 5, currentYear).boxed().collect(Collectors.toList())
            )
        );
        yearSelector.setValue(currentYear);

        Label monthLabel = new Label("Select Month:");
        ComboBox<Month> monthSelector = new ComboBox<>(FXCollections.observableArrayList(Month.values()));
        monthSelector.setValue(LocalDate.now().getMonth());

        Button generateBtn = new Button("Generate Report");
        generateBtn.setMaxWidth(Double.MAX_VALUE);

        generateBtn.setOnAction(e -> {
            Integer year = yearSelector.getValue();
            Month month = monthSelector.getValue();
            if (year == null || month == null) {
                showError("Please select both a year and a month.");
                return;
            }
            try {
                expectingManagerDataType = "report";
                String msg = String.format("GET_MONTHLY_REPORT;%d;%d", year, month.getValue());
                client.sendToServer(msg);
            } catch (IOException ex) {
                showError("Failed to request report from the server.");
            }
        });
        content.getChildren().addAll(titleLabel, yearLabel, yearSelector, monthLabel, monthSelector, generateBtn);
        
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        Scene scene = createMainViewScene("Report Generation", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Monthly Report");
        primaryStage.setMaximized(true);
    }
    
    private void showMonthlyReportPage(ArrayList<MonthlyReportData> reportData) {
        dashboardShown = false;
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Cars Parked");
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Parking Trend for the Month");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Parking Count");
        if (reportData != null) {
            for (MonthlyReportData data : reportData) {
                series.getData().add(new XYChart.Data<>(data.getDate(), data.getParkingCount()));
            }
        }
        lineChart.getData().add(series);
        lineChart.setLegendVisible(false);

        TableView<MonthlyReportData> table = new TableView<>();
        TableColumn<MonthlyReportData, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));
        TableColumn<MonthlyReportData, Number> countCol = new TableColumn<>("Cars Parked");
        countCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getParkingCount()));
        dateCol.setStyle("-fx-alignment: CENTER;");
        countCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().addAll(dateCol, countCol);
        if (reportData != null) {
            table.getItems().addAll(reportData);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No parking activity was recorded for the selected period."));
        
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.getItems().addAll(lineChart, table);
        splitPane.setDividerPositions(0.7);

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Report Selection");
        backButton.getStyleClass().add("nav-button");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> showReportSelectionScreen());
        leftNav.getChildren().add(backButton);

        Scene scene = createMainViewScene("Monthly Parking Activity Report", splitPane, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Monthly Report");
        primaryStage.setMaximized(true);
    }
    
    private void showSubscriberPage(ArrayList<SubscriberInfo> subscribers) {
        dashboardShown = false;

        TableView<SubscriberInfo> table = new TableView<>();
        TableColumn<SubscriberInfo, String> codeCol = new TableColumn<>("Subscription Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("subscriptionCode"));
        TableColumn<SubscriberInfo, String> nameCol = new TableColumn<>("User Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        TableColumn<SubscriberInfo, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        TableColumn<SubscriberInfo, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        TableColumn<SubscriberInfo, Integer> lateCountCol = new TableColumn<>("Late Count");
        lateCountCol.setCellValueFactory(new PropertyValueFactory<>("lateCount"));
        TableColumn<SubscriberInfo, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isFrozen() ? "Frozen" : "Active")
        );
        TableColumn<SubscriberInfo, Void> actionCol = new TableColumn<>("Action");

        Callback<TableColumn<SubscriberInfo, Void>, TableCell<SubscriberInfo, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<SubscriberInfo, Void> call(final TableColumn<SubscriberInfo, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button();
                    {
                        btn.setOnAction(event -> {
                            SubscriberInfo sub = getTableView().getItems().get(getIndex());
                            boolean newFreezeState = !sub.isFrozen();
                            String msg = String.format("SET_FREEZE_STATUS;%s;%s", sub.getSubscriptionCode(), newFreezeState ? "1" : "0");
                            
                            try {
                                client.sendToServer(msg);
                                sub.setFrozen(newFreezeState);
                                getTableView().refresh();
                            } catch (IOException e) {
                                showError("Failed to send command to server: " + e.getMessage());
                            }
                        });
                    }
                    @Override
    /**
     * updateItem method.
     *
     * @param item the item
     * @param empty the empty
     *
     */
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            SubscriberInfo sub = getTableView().getItems().get(getIndex());
                            btn.setText(sub.isFrozen() ? "Unfreeze" : "Freeze");
                            btn.getStyleClass().setAll("button", sub.isFrozen() ? "success-button" : "danger-button");
                            setGraphic(btn);
                            setAlignment(Pos.CENTER);
                        }
                    }
                };
            }
        };
        actionCol.setCellFactory(cellFactory);
        table.getColumns().addAll(codeCol, nameCol, phoneCol, emailCol, lateCountCol, statusCol, actionCol);
        table.getItems().addAll(subscribers);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No subscriber data available"));
        
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        String title;
        if ("manager".equals(client.tableContext)) {
            title = "Manager Dashboard - Subscribers";
            addManagerButtons(leftNav);
        } else {
            title = "Attendant View - Subscribers";
            Button backButton = new Button("Back to Attendant Menu");
            backButton.getStyleClass().add("nav-button");
            backButton.setOnAction(e -> new ParkingAttendantScreen(primaryStage, this).show());
            leftNav.getChildren().add(backButton);
        }

        Scene scene = createMainViewScene(title, table, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Subscribers List");
        primaryStage.setMaximized(true);
    }
    
    private void styleBarChart(BarChart<String, Number> barChart, XYChart.Series<String, Number> series) {
        Node chartPlotBackground = barChart.lookup(".chart-plot-background");
        if (chartPlotBackground != null) {
            chartPlotBackground.setStyle("-fx-background-color: #383838;");
        }
        CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) barChart.getYAxis();
        xAxis.setTickLabelFill(Color.WHITE);
        yAxis.setTickLabelFill(Color.WHITE);
        xAxis.lookup(".axis-label").setStyle("-fx-text-fill: white;");
        yAxis.lookup(".axis-label").setStyle("-fx-text-fill: white;");
        barChart.getData().add(series);
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: #008AE3;");
                }
            }
        });
    }

    private void showSubscriberParkingReportPage(ArrayList<SubscriberParkingData> reportData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Subscriber Name");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Hours Parked");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Subscriber Parking Distribution");
        barChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (reportData != null) {
            for (SubscriberParkingData data : reportData) {
                series.getData().add(new XYChart.Data<>(data.getSubscriberName(), data.getTotalParkedHours()));
            }
        }
        styleBarChart(barChart, series);

        TableView<SubscriberParkingData> table = new TableView<>();
        TableColumn<SubscriberParkingData, String> nameCol = new TableColumn<>("Subscriber Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("subscriberName"));
        TableColumn<SubscriberParkingData, Integer> hoursCol = new TableColumn<>("Total Hours Parked");
        hoursCol.setCellValueFactory(new PropertyValueFactory<>("totalParkedHours"));
        nameCol.setStyle("-fx-alignment: CENTER;");
        hoursCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().addAll(nameCol, hoursCol);
        if (reportData != null) {
            table.setItems(FXCollections.observableArrayList(reportData));
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        SplitPane splitPane = new SplitPane(barChart, table);
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Report Selection");
        backButton.getStyleClass().add("nav-button");
        backButton.setOnAction(e -> showSubscriberParkingReportSelectionScreen());
        leftNav.getChildren().add(backButton);

        Scene scene = createMainViewScene("Total Parking Hours per Subscriber", splitPane, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Subscriber Parking Report");
        primaryStage.setMaximized(true);
    }

    private void showSlotOccupancyReportPage(ArrayList<SlotOccupancyData> reportData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Parking Slot");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Hours Occupied");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Slot Occupancy Distribution");
        barChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (reportData != null) {
            for (SlotOccupancyData data : reportData) {
                series.getData().add(new XYChart.Data<>(data.getParkingSpace(), data.getTotalOccupiedHours()));
            }
        }
        styleBarChart(barChart, series);

        TableView<SlotOccupancyData> table = new TableView<>();
        TableColumn<SlotOccupancyData, String> slotCol = new TableColumn<>("Parking Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("parkingSpace"));
        TableColumn<SlotOccupancyData, Integer> hoursCol = new TableColumn<>("Total Hours Occupied");
        hoursCol.setCellValueFactory(new PropertyValueFactory<>("totalOccupiedHours"));
        slotCol.setStyle("-fx-alignment: CENTER;");
        hoursCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().addAll(slotCol, hoursCol);
        if (reportData != null) {
            table.setItems(FXCollections.observableArrayList(reportData));
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        SplitPane splitPane = new SplitPane(barChart, table);
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Report Selection");
        backButton.getStyleClass().add("nav-button");
        backButton.setOnAction(e -> showSlotOccupancyReportSelectionScreen());
        leftNav.getChildren().add(backButton);

        Scene scene = createMainViewScene("Total Occupancy Hours per Slot", splitPane, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Slot Occupancy Report");
        primaryStage.setMaximized(true);
    }
    
    private void showDailyLateReportSelectionScreen() {
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(400);
        Label titleLabel = new Label("Generate Daily Lates Report");
        titleLabel.getStyleClass().add("title-label");
        Label yearLabel = new Label("Select Year:");
        int currentYear = LocalDate.now().getYear();
        ComboBox<Integer> yearSelector = new ComboBox<>(
            FXCollections.observableArrayList(IntStream.rangeClosed(currentYear - 5, currentYear).boxed().collect(Collectors.toList()))
        );
        yearSelector.setValue(currentYear);
        Label monthLabel = new Label("Select Month:");
        ComboBox<Month> monthSelector = new ComboBox<>(FXCollections.observableArrayList(Month.values()));
        monthSelector.setValue(LocalDate.now().getMonth());
        Button generateBtn = new Button("Generate Report");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setOnAction(e -> {
            Integer year = yearSelector.getValue();
            Month month = monthSelector.getValue();
            if (year == null || month == null) {
                showError("Please select both a year and a month.");
                return;
            }
            try {
                expectingManagerDataType = "daily_late_report";
                String msg = String.format("GET_DAILY_LATENESS_REPORT;%d;%d", year, month.getValue());
                client.sendToServer(msg);
            } catch (IOException ex) {
                showError("Failed to request report from the server.");
            }
        });
        content.getChildren().addAll(titleLabel, yearLabel, yearSelector, monthLabel, monthSelector, generateBtn);
        
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        Scene scene = createMainViewScene("Daily Lates Report", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Daily Lates Report");
        primaryStage.setMaximized(true);
    }
    
    private void showDailyLateReportPage(ArrayList<DailyLateData> reportData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Late Retrievals");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Late Retrievals per Day");
        barChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (reportData != null) {
            for (DailyLateData data : reportData) {
                series.getData().add(new XYChart.Data<>(data.getDate(), data.getLateCount()));
            }
        }
        styleBarChart(barChart, series);

        TableView<DailyLateData> table = new TableView<>();
        TableColumn<DailyLateData, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<DailyLateData, Integer> countCol = new TableColumn<>("Late Count");
        countCol.setCellValueFactory(new PropertyValueFactory<>("lateCount"));
        dateCol.setStyle("-fx-alignment: CENTER;");
        countCol.setStyle("-fx-alignment: CENTER;");
        table.getColumns().addAll(dateCol, countCol);
        if (reportData != null) {
            table.setItems(FXCollections.observableArrayList(reportData));
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        SplitPane splitPane = new SplitPane(barChart, table);
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        Button backButton = new Button("Back to Report Selection");
        backButton.getStyleClass().add("nav-button");
        backButton.setOnAction(e -> showDailyLateReportSelectionScreen());
        leftNav.getChildren().add(backButton);

        Scene scene = createMainViewScene("Monthly Late Retrievals Report", splitPane, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Daily Late Report");
        primaryStage.setMaximized(true);
    }

	/**
	* MODIFIED: This is an example of a refactored report selection screen with the standard UI.
	*/
    private void showSubscriberParkingReportSelectionScreen() {
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(400);
        Label titleLabel = new Label("Generate Subscriber Parking Report");
        titleLabel.getStyleClass().add("title-label");
        Label yearLabel = new Label("Select Year:");
        int currentYear = LocalDate.now().getYear();
        ComboBox<Integer> yearSelector = new ComboBox<>(
            FXCollections.observableArrayList(IntStream.rangeClosed(currentYear - 5, currentYear).boxed().collect(Collectors.toList()))
        );
        yearSelector.setValue(currentYear);
        Label monthLabel = new Label("Select Month:");
        ComboBox<Month> monthSelector = new ComboBox<>(FXCollections.observableArrayList(Month.values()));
        monthSelector.setValue(LocalDate.now().getMonth());
        Button generateBtn = new Button("Generate Report");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setOnAction(e -> {
            Integer year = yearSelector.getValue();
            Month month = monthSelector.getValue();
            if (year == null || month == null) {
                showError("Please select both a year and a month.");
                return;
            }
            try {
                expectingManagerDataType = "subscriber_parking_report";
                String msg = String.format("GET_SUBSCRIBER_PARKING_REPORT;%d;%d", year, month.getValue());
                client.sendToServer(msg);
            } catch (IOException ex) {
                showError("Failed to request report from the server.");
            }
        });
        content.getChildren().addAll(titleLabel, yearLabel, yearSelector, monthLabel, monthSelector, generateBtn);
        
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        Scene scene = createMainViewScene("Subscriber Parking Report", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Subscriber Parking Report");
        primaryStage.setMaximized(true);
    }

    private void showSlotOccupancyReportSelectionScreen() {
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(400);
        Label titleLabel = new Label("Generate Slot Occupancy Report");
        titleLabel.getStyleClass().add("title-label");
        Label yearLabel = new Label("Select Year:");
        int currentYear = LocalDate.now().getYear();
        ComboBox<Integer> yearSelector = new ComboBox<>(
            FXCollections.observableArrayList(IntStream.rangeClosed(currentYear - 5, currentYear).boxed().collect(Collectors.toList()))
        );
        yearSelector.setValue(currentYear);
        Label monthLabel = new Label("Select Month:");
        ComboBox<Month> monthSelector = new ComboBox<>(FXCollections.observableArrayList(Month.values()));
        monthSelector.setValue(LocalDate.now().getMonth());
        Button generateBtn = new Button("Generate Report");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setOnAction(e -> {
            Integer year = yearSelector.getValue();
            Month month = monthSelector.getValue();
            if (year == null || month == null) {
                showError("Please select both a year and a month.");
                return;
            }
            try {
                expectingManagerDataType = "slot_occupancy_report";
                String msg = String.format("GET_SLOT_OCCUPANCY_REPORT;%d;%d", year, month.getValue());
                client.sendToServer(msg);
            } catch (IOException ex) {
                showError("Failed to request report from the server.");
            }
        });
        content.getChildren().addAll(titleLabel, yearLabel, yearSelector, monthLabel, monthSelector, generateBtn);
        
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        Scene scene = createMainViewScene("Slot Occupancy Report", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Slot Occupancy Report");
        primaryStage.setMaximized(true);
    }
    
    private void showFutureSlotDatePicker() {
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(400);

        Label titleLabel = new Label("View Future Slot Occupancy");
        titleLabel.getStyleClass().add("title-label");

        Label dateLabel = new Label("Select Date:");
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1)); // Default to tomorrow

        Button viewButton = new Button("View Occupancy for Date");
        viewButton.setMaxWidth(Double.MAX_VALUE);

        viewButton.setOnAction(e -> {
            LocalDate selectedDate = datePicker.getValue();
            if (selectedDate == null) {
                showError("Please select a date.");
                return;
            }
            
            this.requestedFutureDate = selectedDate.toString(); 

            try {
                expectingManagerDataType = "future_slots";
                client.sendToServer("GET_FUTURE_SLOTS;" + this.requestedFutureDate);
            } catch (IOException ex) {
                showError("Failed to request future slot data from the server.");
            }
        });

        content.getChildren().addAll(titleLabel, dateLabel, datePicker, viewButton);

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        Scene scene = createMainViewScene("Future Slot Viewer", content, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - Future Slot Viewer");
        primaryStage.setMaximized(true);
    }
    
    private void showFutureSlotsPage(ArrayList<Integer> occupiedSlots, String date) {
        SlotViewer viewer = new SlotViewer(occupiedSlots);
        GridPane grid = viewer.getGrid();

        VBox content = new VBox(20, grid);
        content.setAlignment(Pos.CENTER);

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        String title = "Slot Occupancy for " + date;
        Scene scene = createMainViewScene(title, content, leftNav);
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - " + title);
        primaryStage.setMaximized(true);
    }

    private void showNoOrdersFoundPage(String date) {
        // Main content pane
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        // Style it to have a red background
        content.setStyle("-fx-background-color: #5c2c2c; -fx-padding: 20px; -fx-background-radius: 8px;"); // A dark, less glaring red

        // The message to display
        Label messageLabel = new Label("No Scheduled Orders Found");
        messageLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label dateLabel = new Label("for " + date);
        dateLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #cccccc;");

        content.getChildren().addAll(messageLabel, dateLabel);

        // Standard left navigation for the manager
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        // Use the standard scene creator
        String title = "Slot Occupancy for " + date;
        Scene scene = createMainViewScene(title, content, leftNav);

        primaryStage.setScene(scene);
        primaryStage.setTitle("BPark - " + title);
        primaryStage.setMaximized(true);
    }

/**
 * This class represents the MyClient.
 */
    public class MyClient extends AbstractClient {
        private List<Integer> occupiedSlots = new ArrayList<>();
        private boolean wasUpdate = false;
        public String tableContext = "manager";

        public MyClient(String host, int port) {
            super(host, port);
        }

    /**
     * handleMessageFromClientUI method.
     *
     * @param message the message
     * 
     */
        public void handleMessageFromClientUI(String message) {
            try {
                sendToServer(message);
            } catch (IOException e) {
                showError("Failed to send message to server: " + e.getMessage());
            }
        }
        

        // Assuming these classes and methods exist from the original context
        // import javafx.application.Platform;
        // import javafx.scene.control.Alert;

        protected void handleMessageFromServer(Object msg) {
            Platform.runLater(() -> {
                if (msg instanceof String message) {
                    processStringMessage(message);
                } else if (msg instanceof SubscriberInfo newInfo) {
                    processSubscriberInfoUpdate(newInfo);
                } else if (msg instanceof ArrayList<?> list) {
                    processListData(list);
                } else {
                    System.out.println("Unknown message type received: " + msg.getClass().getName());
                }
            });
        }

        /**
         * Processes all incoming messages that are of type String.
         * It parses the command and delegates to the appropriate handler.
         */
        private void processStringMessage(String message) {
            if (message.equalsIgnoreCase("SHUTDOWN")) {
                handleShutdown();
                return;
            }

            // Use a helper to avoid repetitive substring calls
            String[] parts = message.split("[:;]", 2);
            String command = parts[0];
            String payload = (parts.length > 1) ? parts[1] : "";

            switch (command) {
                case "PARK_FULL" -> showError("Parking is full. Please come back later.");
                case "PARK_FAILED" -> showError("Parking Failed: " + payload);
                case "PARK_CONFIRMED" -> handleParkConfirmed(payload);
                case "PARK_WITH_RESERVATION_FAILED" -> showError("Parking Failed: " + payload);
                case "FUTURE_PARK_FAILED" -> showError("Failed to book future parking: " + payload);
                case "EXTEND_SUCCESS" -> handleExtendSuccess(payload);
                case "EXTEND_FAILED" -> showError("Failed to extend parking: " + payload);
                case "FUTURE_PARK_SUCCESS" -> handleFutureParkSuccess(payload);
                case "LOGIN_FAILED" -> handleLoginResult(payload, false);
                case "LOGIN_SUCCESS" -> handleLoginResult("Login successful.", true);
                case "UPDATE_FAILED" -> showError("Failed to update information. Please try again.");
                case "UPDATE_SUCCESS" -> wasUpdate = true;
                case "RELEASE_SUCCESS" -> handleReleaseSuccess(payload);
                case "RELEASE_FAILED" -> showError("Failed to release vehicle: " + payload);
                case "FORGOT_CODE_SUCCESS" -> showSuccess("An email with your confirmation code has been sent.");
                case "FORGOT_CODE_FAILED" -> showError("Could not retrieve code: " + payload);
                default -> System.out.println("Unknown string command: " + command);
            }
        }

        /**
         * Processes all incoming messages that are ArrayLists.
         * It checks the list content and delegates to the correct handler.
         */
        private void processListData(ArrayList<?> list) {
            if (list.isEmpty()) {
                handleEmptyListResponse();
                return;
            }

            Object firstElement = list.get(0);

            // Using instanceof with pattern matching (available in modern Java versions)
            // would be even cleaner, but this is universally compatible.
            if (firstElement instanceof Integer) {
                handleIntegerList((ArrayList<Integer>) list);
            } else if (firstElement instanceof SubscriberParkingData) {
                showSubscriberParkingReportPage((ArrayList<SubscriberParkingData>) list);
            } else if (firstElement instanceof SlotOccupancyData) {
                showSlotOccupancyReportPage((ArrayList<SlotOccupancyData>) list);
            } else if (firstElement instanceof DailyLateData) {
                showDailyLateReportPage((ArrayList<DailyLateData>) list);
            } else if (firstElement instanceof MonthlyReportData) {
                showMonthlyReportPage((ArrayList<MonthlyReportData>) list);
            } else if (firstElement instanceof ActivityInfo) {
                handleActivityInfoList((ArrayList<ActivityInfo>) list);
            } else if (firstElement instanceof OrderInfo) {
                handleOrderInfoList((ArrayList<OrderInfo>) list);
            } else if (firstElement instanceof SubscriberInfo) {
                showSubscriberPage((ArrayList<SubscriberInfo>) list);
            }
        }

        /**
         * Handles the specific logic for when an empty list is received from the server,
         * routing to the correct UI page based on the expected data type.
         */
        private void handleEmptyListResponse() {
            switch (expectingManagerDataType) {
                case "future_slots" -> showNoOrdersFoundPage(requestedFutureDate);
                case "subscriber_parking_report" -> showSubscriberParkingReportPage(new ArrayList<>());
                case "slot_occupancy_report" -> showSlotOccupancyReportPage(new ArrayList<>());
                case "daily_late_report" -> showDailyLateReportPage(new ArrayList<>());
                case "activities" -> showAllActivityHistoryPage(new ArrayList<>());
                case "orders" -> showManagerActiveParkingPage(new ArrayList<>());
                case "scheduled_orders" -> showManagerScheduledOrdersPage(new ArrayList<>());
                case "subscribers" -> showSubscriberPage(new ArrayList<>());
                case "report" -> showMonthlyReportPage(new ArrayList<>());
                default -> { // Not a manager data type, check other flags
                    if (expectingSlots) {
                        occupiedSlots.clear();
                        if ("VIEW_SLOTS".equals(ClientUi.this.role)) showSlotsAndGoBack(new ArrayList<>());
                        else if(dashboardShown) updateClientDashboardSlots(new ArrayList<>());
                    } else if (expectingHistory) {
                        showHistoryPage(new ArrayList<>());
                    }
                }
            }
            // Reset state flags after handling
            expectingManagerDataType = null;
            expectingSlots = false;
            expectingHistory = false;
        }

        // --- Specific Handler Methods ---

        private void handleShutdown() {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Lost");
            alert.setHeaderText("The server has been shut down.");
            alert.setContentText("The application will now terminate.");
            alert.showAndWait();
            quit();
            Platform.exit();
            System.exit(0);
        }

        private void handleParkConfirmed(String payload) {
            String[] parts = payload.split(":");
            int slot = Integer.parseInt(parts[0]);
            String code = parts[1];
            if (!occupiedSlots.contains(slot)) {
                occupiedSlots.add(slot);
            }
            showSlotFlashingScreen(slot, code);
        }

        private void handleExtendSuccess(String message) {
            showSuccess(message);
            showClientDashboard();
        }

        private void handleFutureParkSuccess(String code) {
            showSuccess("Your future parking has been booked successfully!\nYour confirmation code is: " + code);
            showClientDashboard();
        }

        private void handleLoginResult(String text, boolean success) {
            resultLabel.setText(text);
            String styleClass = success ? "result-label-success" : "result-label-error";
            resultLabel.getStyleClass().setAll("result-label", styleClass);
        }

        private void handleReleaseSuccess(String payload) {
            showSuccess("Vehicle released successfully!");
            try {
                int releasedSlot = Integer.parseInt(payload);
                occupiedSlots.remove(Integer.valueOf(releasedSlot));
                showSlotFlashingScreenForRelease(releasedSlot, new ArrayList<>(occupiedSlots));
            } catch (NumberFormatException e) {
                // If payload is not a valid slot number, just go to dashboard
                showClientDashboard();
            }
        }

        private void processSubscriberInfoUpdate(SubscriberInfo newInfo) {
            subscriberInfo = newInfo;
            if (wasUpdate) {
                showSuccess("Your information was updated successfully!");
                wasUpdate = false;
            }
            showClientDashboard();
        }

        private void handleIntegerList(ArrayList<Integer> receivedSlots) {
            if (expectingSlots) {
                occupiedSlots = receivedSlots;
                expectingSlots = false; // Reset flag
                if ("VIEW_SLOTS".equals(ClientUi.this.role)) {
                    showSlotsAndGoBack(new ArrayList<>(occupiedSlots));
                } else if (dashboardShown) {
                    updateClientDashboardSlots(new ArrayList<>(occupiedSlots));
                }
            } else if ("future_slots".equals(expectingManagerDataType)) {
                expectingManagerDataType = null; // Reset flag
                showFutureSlotsPage(receivedSlots, requestedFutureDate);
            }
        }

        private void handleActivityInfoList(ArrayList<ActivityInfo> list) {
            if (expectingHistory) {
                showHistoryPage(list);
            } else if ("activities".equals(expectingManagerDataType)) {
                showAllActivityHistoryPage(list);
            }
            // Reset flags
            expectingHistory = false;
            expectingManagerDataType = null;
        }

        private void handleOrderInfoList(ArrayList<OrderInfo> list) {
            if ("orders".equals(expectingManagerDataType)) {
                showManagerActiveParkingPage(list);
            } else if ("scheduled_orders".equals(expectingManagerDataType)) {
                showManagerScheduledOrdersPage(list);
            }
            // Reset flag
            expectingManagerDataType = null;
        }
        
        private void showSlotFlashingScreen(int slot, String code) {
            dashboardShown = false;
            
            Label slotLabel = new Label("Slot Assigned: #" + slot);
            slotLabel.getStyleClass().add("header-label");
            Label codeLabel = new Label("Your Confirmation Code: " + code);
            codeLabel.getStyleClass().add("result-label-success");

            SlotViewer viewer = new SlotViewer(occupiedSlots, slot);
            VBox content = new VBox(20, slotLabel, codeLabel, viewer.getGrid());
            content.setAlignment(Pos.CENTER);

            VBox leftNav = new VBox(15);
            leftNav.getStyleClass().add("side-nav-pane");
            Button backBtn = new Button("Back to Dashboard");
            backBtn.getStyleClass().add("nav-button");
            backBtn.setOnAction(e -> showClientDashboard());
            leftNav.getChildren().add(backBtn);

            Scene scene = createMainViewScene("Parking Confirmed!", content, leftNav);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
        }
        
        private void showSlotFlashingScreenForRelease(int releasedSlot, ArrayList<Integer> currentOccupiedSlots) {
            dashboardShown = false;
            Label slotLabel = new Label("Slot #" + releasedSlot + " is now free.");
            slotLabel.getStyleClass().add("header-label");
            SlotViewer viewer = new SlotViewer(currentOccupiedSlots, releasedSlot, SlotViewer.SlotFlashType.RELEASE_CAR);
            VBox content = new VBox(20, slotLabel, viewer.getGrid());
            content.setAlignment(Pos.CENTER);

            VBox leftNav = new VBox(15);
            leftNav.getStyleClass().add("side-nav-pane");
            Button backBtn = new Button("Back to Dashboard");
            backBtn.getStyleClass().add("nav-button");
            backBtn.setOnAction(e -> showClientDashboard());
            leftNav.getChildren().add(backBtn);

            Scene scene = createMainViewScene("Vehicle Released!", content, leftNav);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
        }
        
        @Override
        protected void connectionEstablished() { Platform.runLater(ClientUi.this::showRoleSelectionScreen); }
        @Override
        protected void connectionClosed() { Platform.runLater(() -> { showError("Connection to server has been lost."); showConnectionScreen(); }); }
    }
    
    private void showManagerActiveParkingPage(ArrayList<OrderInfo> orders) {
        dashboardShown = false;

        TableView<OrderInfo> table = new TableView<>();
        TableColumn<OrderInfo, String> orderNumberCol = new TableColumn<>("Order Num");
        orderNumberCol.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        TableColumn<OrderInfo, String> parkingSpaceCol = new TableColumn<>("Space");
        parkingSpaceCol.setCellValueFactory(new PropertyValueFactory<>("parkingSpace"));
        TableColumn<OrderInfo, String> subscriberIdCol = new TableColumn<>("Subscriber ID");
        subscriberIdCol.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        TableColumn<OrderInfo, String> confirmationCodeCol = new TableColumn<>("Confirmation Code");
        confirmationCodeCol.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        TableColumn<OrderInfo, String> placingTimeCol = new TableColumn<>("Parking Start Time");
        placingTimeCol.setCellValueFactory(new PropertyValueFactory<>("timeOfPlacingOrder"));
        TableColumn<OrderInfo, String> endTimeCol = new TableColumn<>("Parking End Time");
        endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endParkTime"));
        table.getColumns().addAll(orderNumberCol, parkingSpaceCol, subscriberIdCol, confirmationCodeCol, placingTimeCol, endTimeCol);
        table.getItems().addAll(orders);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No active parking sessions found."));
        table.getColumns().forEach(col -> col.setStyle("-fx-alignment: CENTER;"));

        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        String title;
        if ("manager".equals(client.tableContext)) {
            title = "Manager - Active Parking";
            addManagerButtons(leftNav);
        } else {
            title = "Attendant - Active Parking";
            Button backButton = new Button("Back to Attendant Menu");
            backButton.getStyleClass().add("nav-button");
            backButton.setOnAction(e -> new ParkingAttendantScreen(primaryStage, this).show());
            leftNav.getChildren().add(backButton);
        }

        Scene scene = createMainViewScene(title, table, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Active Parking");
        primaryStage.setMaximized(true);
    }

    private void showManagerScheduledOrdersPage(ArrayList<OrderInfo> orders) {
        dashboardShown = false;

        TableView<OrderInfo> table = new TableView<>();
        TableColumn<OrderInfo, String> subscriberIdCol = new TableColumn<>("Subscriber ID");
        subscriberIdCol.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        TableColumn<OrderInfo, String> userNameCol = new TableColumn<>("User Name");
        userNameCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        TableColumn<OrderInfo, String> scheduledTimeCol = new TableColumn<>("Scheduled Time");
        scheduledTimeCol.setCellValueFactory(new PropertyValueFactory<>("scheduledTime"));
        TableColumn<OrderInfo, String> futureSpotCol = new TableColumn<>("Assigned Spot");
        futureSpotCol.setCellValueFactory(new PropertyValueFactory<>("futureParkingSpot"));
        TableColumn<OrderInfo, String> confirmationCodeCol = new TableColumn<>("Confirmation Code");
        confirmationCodeCol.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        table.getColumns().addAll(subscriberIdCol, userNameCol, scheduledTimeCol, futureSpotCol, confirmationCodeCol);
        table.getItems().addAll(orders);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No future parking orders found."));
        table.getColumns().forEach(col -> col.setStyle("-fx-alignment: CENTER;"));
        
        VBox leftNav = new VBox(15);
        leftNav.getStyleClass().add("side-nav-pane");
        addManagerButtons(leftNav);

        Scene scene = createMainViewScene("Manager - Scheduled Orders", table, leftNav);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Scheduled Parking Orders");
        primaryStage.setMaximized(true);
    }

    private void addManagerButtons(VBox navBox) {
        Button showScheduledBtn = new Button("Scheduled Orders");
        showScheduledBtn.getStyleClass().add("nav-button");
        showScheduledBtn.setMaxWidth(Double.MAX_VALUE);
        showScheduledBtn.setOnAction(e -> {
            try {
                expectingManagerDataType = "scheduled_orders";
                client.sendToServer("get_all_scheduled_orders");
            } catch (IOException ex) {
                showError("Failed to request scheduled orders data.");
            }
        });

        Button showActiveParkingBtn = new Button("Active Parking");
        showActiveParkingBtn.getStyleClass().add("nav-button");
        showActiveParkingBtn.setMaxWidth(Double.MAX_VALUE);
        showActiveParkingBtn.setOnAction(e -> {
            try {
                expectingManagerDataType = "orders";
                client.sendToServer("get_all_orders");
            } catch (IOException ex) {
                showError("Failed to request active parking data.");
            }
        });
        
        Button showSubscribersBtn = new Button("Subscribers");
        showSubscribersBtn.getStyleClass().add("nav-button");
        showSubscribersBtn.setMaxWidth(Double.MAX_VALUE);
        showSubscribersBtn.setOnAction(e -> {
            try {
                expectingManagerDataType = "subscribers";
                client.sendToServer("get_all_subscribers");
            } catch (IOException ex) {
                showError("Failed to request subscriber data.");
            }
        });
        
        Button showActivityHistoryBtn = new Button("Activity History");
        showActivityHistoryBtn.getStyleClass().add("nav-button");
        showActivityHistoryBtn.setMaxWidth(Double.MAX_VALUE);
        showActivityHistoryBtn.setOnAction(e -> {
            try {
                expectingManagerDataType = "activities";
                client.sendToServer("GET_ALL_ACTIVITY_LOGS");
            } catch (IOException ex) {
                showError("Failed to request activity history.");
            }
        });

        Button futureViewerBtn = new Button("Future Slot Viewer");
        futureViewerBtn.getStyleClass().add("nav-button");
        futureViewerBtn.setMaxWidth(Double.MAX_VALUE);
        futureViewerBtn.setOnAction(e -> showFutureSlotDatePicker());

        MenuButton reportsMenu = new MenuButton("Reports");
        reportsMenu.getStyleClass().add("nav-button");
        reportsMenu.setMaxWidth(Double.MAX_VALUE);
        MenuItem parkingReport = new MenuItem("Monthly Parking Report");
        parkingReport.setOnAction(e -> showReportSelectionScreen());
        MenuItem subscriberHours = new MenuItem("Subscriber Parking Hours");
        subscriberHours.setOnAction(e -> showSubscriberParkingReportSelectionScreen());
        MenuItem slotOccupancy = new MenuItem("Slot Occupancy Hours");
        slotOccupancy.setOnAction(e -> showSlotOccupancyReportSelectionScreen());
        MenuItem dailyLates = new MenuItem("Daily Late Retrievals");
        dailyLates.setOnAction(e -> showDailyLateReportSelectionScreen());
        reportsMenu.getItems().addAll(parkingReport, subscriberHours, slotOccupancy, dailyLates);

        Pane spacer = new Pane();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Back to Roles");
        logoutBtn.getStyleClass().addAll("nav-button", "secondary-button");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> showRoleSelectionScreen());

        navBox.getChildren().addAll(showScheduledBtn, showActiveParkingBtn, showSubscribersBtn, showActivityHistoryBtn, futureViewerBtn, reportsMenu, spacer, logoutBtn);
    }
}