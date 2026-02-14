package gui; // Make sure this package name matches your client's package

import common.OrderInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.ArrayList;

public class OrderHistoryController {

    @FXML
    private TableView<OrderInfo> historyTable;

    @FXML
    private TableColumn<OrderInfo, String> orderDateColumn;

    @FXML
    private TableColumn<OrderInfo, String> parkingSlotColumn;

    @FXML
    private TableColumn<OrderInfo, String> confirmationCodeColumn;

    @FXML
    private TableColumn<OrderInfo, String> orderNumberColumn;

    @FXML
    public void initialize() {
        // Sets up the columns to know which field from the OrderInfo object to display.
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        parkingSlotColumn.setCellValueFactory(new PropertyValueFactory<>("parkingSpace"));
        confirmationCodeColumn.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        orderNumberColumn.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));

        //Center the content in all columns
        orderDateColumn.setStyle("-fx-alignment: CENTER;");
        parkingSlotColumn.setStyle("-fx-alignment: CENTER;");
        confirmationCodeColumn.setStyle("-fx-alignment: CENTER;");
        orderNumberColumn.setStyle("-fx-alignment: CENTER;");
    }

    /**
     * This method is called from the main client controller to pass the history data
     * into this controller and display it in the table.
     * @param history The list of orders received from the server.
     */
    public void populateHistory(ArrayList<OrderInfo> history) {
        // Use Platform.runLater to ensure UI updates happen on the main JavaFX thread.
        Platform.runLater(() -> {
            historyTable.setItems(FXCollections.observableArrayList(history));
        });
    }
}