package common;

import java.io.Serializable;

/**
 * OrderInfo is a versatile data transfer object used to carry information
 * about both active parking sessions (from the 'activeparking' table) and
 * scheduled future orders (from the 'parkingorders' table).
 * Some fields may be null depending on the context.
 */
public class OrderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    // Fields from 'activeparking' table
    private String parkingSpace;
    private String orderNumber;
    private String confirmationCode;
    private String subscriberId;
    private String timeOfPlacingOrder; // Corresponds to time_of_placing_an_order
    private String endParkTime;        // Corresponds to endParkTime
    private String orderDate;          // Corresponds to order_date

    // Fields from 'parkingorders' table
    private String scheduledTime;      // Corresponds to scheduled_time
    private String futureParkingSpot;  // Corresponds to futureParkingSpot

    // Helper fields for other functionalities
    private String userName;           // For displaying user names in tables
    private String userNameForEmail;   // For sending emails
    private String userEmailForEmail;  // For sending emails

    /**
     * Constructor for Active Parking Orders from the 'activeparking' table.
     */
    public OrderInfo(String parkingSpace, String orderNumber, String orderDate, String confirmationCode, String subscriberId, String timeOfPlacingOrder, String endParkTime) {
        this.parkingSpace = parkingSpace;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.confirmationCode = confirmationCode;
        this.subscriberId = subscriberId;
        this.timeOfPlacingOrder = timeOfPlacingOrder;
        this.endParkTime = endParkTime;
    }

    /**
     * Constructor for Scheduled Future Orders from the 'parkingorders' table.
     */
    public OrderInfo(String subscriberId, String userName, String scheduledTime, String futureParkingSpot, String confirmationCode) {
        this.subscriberId = subscriberId;
        this.userName = userName;
        this.scheduledTime = scheduledTime;
        this.futureParkingSpot = futureParkingSpot;
        this.confirmationCode = confirmationCode;
    }

    // Getters
    public String getParkingSpace() { return parkingSpace; }
    public String getOrderNumber() { return orderNumber; }
    public String getConfirmationCode() { return confirmationCode; }
    public String getSubscriberId() { return subscriberId; }
    public String getTimeOfPlacingOrder() { return timeOfPlacingOrder; }
    public String getEndParkTime() { return endParkTime; }
    public String getOrderDate() { return orderDate; }
    public String getScheduledTime() { return scheduledTime; }
    public String getFutureParkingSpot() { return futureParkingSpot; }
    public String getUserName() { return userName; }
    public String getUserNameForEmail() { return userNameForEmail; }
    public String getUserEmailForEmail() { return userEmailForEmail; }

    // Setters
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserNameForEmail(String userNameForEmail) { this.userNameForEmail = userNameForEmail; }
    public void setUserEmailForEmail(String userEmailForEmail) { this.userEmailForEmail = userEmailForEmail; }
    public void setOrderNumber(String orderNumber) {this.orderNumber = orderNumber;}
    // Legacy support for older parts of the code
    public String getDateOfPlacingOrder() { return timeOfPlacingOrder; }
}