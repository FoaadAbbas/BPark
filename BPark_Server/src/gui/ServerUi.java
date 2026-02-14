package gui;

import backend.EchoServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

/**
 * This class represents the ServerUi.
 */
public class ServerUi extends Application {

    
    /**
     * clientExists method.
     *
     * @param ip the ip
     * @return result
     */
    public static boolean clientExists(String ip) {
        for (ClientInfo c : tableView.getItems()) {
            if (c.getIp().equals(ip)) return true;
        }
        return false;
    }
    


    /**
     * updateClientStatus method.
     *
     * @param ip the ip
     * @param status the status
     * 
     */
    public static void updateClientStatus(String ip, String status) {
        for (ClientInfo c : tableView.getItems()) {
            if (c.getIp().equals(ip)) {
                c.setStatus(status);
                tableView.refresh();
                break;
            }
        }
    }


    private static TableView<ClientInfo> tableView;
    private Stage primaryStage;
    private EchoServer server;

    @Override
    /**
     * start method.
     *
     * @param primaryStage the primaryStage
     * 
     */
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("BPark Server");
        
        primaryStage.setOnCloseRequest(event -> {
            if (server != null) server.gracefulShutdown();
            Platform.exit();
            System.exit(0);
        });
showPortSelectionScreen();
    }

    private Scene createStyledScene(Region layout) {
        StackPane root = new StackPane(layout);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 800, 600);
        // Ensure you have modern-theme.css in the Server package directory
        scene.getStylesheets().add(getClass().getResource("modern-theme.css").toExternalForm());
        return scene;
    }

    private void showPortSelectionScreen() {
        VBox content = new VBox(15);
        content.getStyleClass().add("content-pane");
        content.setMaxWidth(400);
        content.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Server Configuration");
        titleLabel.getStyleClass().add("title-label");
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        Label portLabel = new Label("Enter Port:");
        TextField portField = new TextField("5555");
        portField.setPromptText("Port Number");

        Button startButton = new Button("Start Server");
        startButton.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(startButton, new Insets(10, 0, 0, 0));

        startButton.setOnAction(e -> {
            String portText = portField.getText();
            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException ex) {
                showAlert("Invalid Port", "Please enter a valid port number.");
                return;
            }
            startServer(port);
        });

        content.getChildren().addAll(titleLabel, portLabel, portField, startButton);
        
        Scene scene = createStyledScene(content);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showServerDashboard() {
        VBox layout = new VBox(20);
        layout.getStyleClass().add("content-pane");

        Label title = new Label("Server Dashboard - Connected Clients");
        title.getStyleClass().add("title-label");

        setupTable();

        layout.getChildren().addAll(title, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        Scene scene = createStyledScene(layout);
        primaryStage.setScene(scene);
    }

    private void setupTable() {
        tableView = new TableView<>();
        TableColumn<ClientInfo, String> ipColumn = new TableColumn<>("IP Address");
        ipColumn.setCellValueFactory(data -> data.getValue().ipProperty());
        ipColumn.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClientInfo, String> hostColumn = new TableColumn<>("Host");
        hostColumn.setCellValueFactory(data -> data.getValue().hostProperty());
        hostColumn.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClientInfo, String> portColumn = new TableColumn<>("Port");
        portColumn.setCellValueFactory(data -> data.getValue().portProperty());
        portColumn.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClientInfo, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setStyle("-fx-alignment: CENTER;");
        
        tableView.getColumns().addAll(ipColumn, hostColumn, portColumn, statusColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startServer(int port) {
        server = new EchoServer(port);
        new Thread(() -> {
            try {
                server.listen();
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Server Error", "Could not listen for clients on port " + port));
            }
        }).start();
        showServerDashboard();
    }

    /**
     * addClient method.
     *
     * @param ip the ip
     * @param host the host
     * @param port the port
     * @param status the status
     */
    public static void addClient(String ip, String host, int port, String status) {
        ClientInfo client = new ClientInfo(ip, host, port, status);
        Platform.runLater(() -> {
            if (tableView != null) {
                tableView.getItems().add(client);
            }
        });
    }

    /**
     * updateClientStatus method.
     *
     * @param ip the ip
     * @param host the host
     * @param status the status
     * 
     */
    public static void updateClientStatus(String ip, String host, String status) {
        Platform.runLater(() -> {
            if (tableView != null) {
                for (ClientInfo client : tableView.getItems()) {
                    if (client.getIp().equals(ip) && client.getHost().equals(host)) {
                        client.setStatus(status);
                        break;
                    }
                }
                tableView.refresh(); // Refresh to show status change
            }
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
    
    public static class ClientInfo {
        private final SimpleStringProperty ip, host, status, port;
        public ClientInfo(String ip, String host, int port, String status) {
            this.ip = new SimpleStringProperty(ip);
            this.host = new SimpleStringProperty(host);
            this.port = new SimpleStringProperty(String.valueOf(port));
            this.status = new SimpleStringProperty(status);
        }
    /**
     * getIp method.
     *
     * @return result
     */
        public String getIp() { return ip.get(); }
    /**
     * ipProperty method.
     *
     * @return result
     */
        public SimpleStringProperty ipProperty() { return ip; }
    /**
     * getHost method.
     *
     * @return result
     */
        public String getHost() { return host.get(); }
    /**
     * hostProperty method.
     *
     * @return result
     */
        public SimpleStringProperty hostProperty() { return host; }
    /**
     * getStatus method.
     *
     * @return result
     */
        public String getStatus() { return status.get(); }
    /**
     * statusProperty method.
     *
     * @return result
     */
        public SimpleStringProperty statusProperty() { return status; }
    /**
     * setStatus method.
     *
     * @param status the status
     * 
     */
        public void setStatus(String status) { this.status.set(status); }
    /**
     * getPort method.
     *
     * @return result
     */
        public String getPort() { return port.get(); }
    /**
     * portProperty method.
     *
     * @return result
     */
        public SimpleStringProperty portProperty() { return port; }
    }
}