package backend;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

import common.ActivityInfo;
import common.DailyLateData;
import common.MonthlyReportData;
import common.OrderInfo;
import common.SlotOccupancyData;
import common.SubscriberInfo;
import common.SubscriberParkingData;
import gui.ServerUi;
import ocsf.server.*;

/**
 * Represents the EchoServer class.
 */
public class EchoServer extends AbstractServer {
    private DBController db;
    private final Map<ConnectionToClient, SubscriberInfo> loggedInSubscribers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public EchoServer(int port) {
        super(port);
        db = new DBController();
        startReminderService();
        startCancellationService();
    }
    /**
     * startCancellationService method.
     */
    private void startCancellationService() {
        Runnable cancellationTask = () -> {
            try {
                // This is the method you already wrote in DBController!
                List<OrderInfo> cancelledOrders = db.checkAndCancelLateReservations();

                if (!cancelledOrders.isEmpty()) {
                    System.out.println("Cancellation service: Found and cancelled " + cancelledOrders.size() + " late reservations.");

                    // Loop through each cancelled order to send an email and handle penalties
                    for (OrderInfo order : cancelledOrders) {
                        // Send the cancellation email
                        EmailService.sendLateReservationCancellationEmail(
                            order.getUserEmailForEmail(),
                            order.getUserNameForEmail(),
                            order.getConfirmationCode(),
                            order.getScheduledTime()
                        );

                        // Apply penalty: Increment late count and potentially freeze the account
                        db.incrementLateCountAndFreeze(order.getSubscriberId());

                        // Check if the user was frozen as a result of this cancellation
                        SubscriberInfo subInfo = DBController.findSubscriberByCode(order.getSubscriberId());
                        if (subInfo != null && subInfo.isFrozen()) {
                            // If frozen, send the account frozen notification email
                            EmailService.sendAccountFrozenEmail(
                                subInfo.getEmail(),
                                subInfo.getUserName(),
                                subInfo.getLateCount()
                            );
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in cancellation service: " + e.getMessage());
                e.printStackTrace();
            }
        };
        // Schedule the task to run every minute, starting 1 minute after the server starts.
        scheduler.scheduleAtFixedRate(cancellationTask, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * startReminderService method.
     */
    private void startReminderService() {
        Runnable reminderTask = () -> {
            try {
                System.out.println("Reminder service running...");
                ArrayList<OrderInfo> ordersToRemind = db.getOrdersForReminder();
                if (!ordersToRemind.isEmpty()) {
                    System.out.println("Found " + ordersToRemind.size() + " orders needing a reminder.");
                    for (OrderInfo order : ordersToRemind) {
                        EmailService.sendReminderEmail(
                            order.getUserEmailForEmail(),
                            order.getUserNameForEmail(),
                            order.getConfirmationCode(),
                            order.getScheduledTime()
                        );
                        db.markReminderAsSent(order.getConfirmationCode());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in reminder service: " + e.getMessage());
                e.printStackTrace();
            }
        };
        scheduler.scheduleAtFixedRate(reminderTask, 1, 1, TimeUnit.MINUTES);
    }
    
    @Override
    /**
     * clientDisconnected method.
     * @param client the client
     */
    protected void clientDisconnected(ConnectionToClient client) {
        if (client != null) {
            SubscriberInfo sub = loggedInSubscribers.remove(client);
            if (sub != null) {
                System.out.println("Subscriber " + sub.getSubscriptionCode() + " disconnected.");
            }
            updateClientAsDisconnected(client.getInetAddress());
        }
    }

    @Override
    /**
     * clientConnected method.
     * @param client the client
     */
    protected void clientConnected(ConnectionToClient client) {
        try {
            if (client.getInetAddress() != null) {
                final String ip = client.getInetAddress().getHostAddress();
                final String host = client.getInetAddress().getHostName();
                final int port = this.getPort();
                Platform.runLater(() -> {
                    if (ServerUi.clientExists(ip)) {
                        ServerUi.updateClientStatus(ip, "Connected");
                    } else {
                        ServerUi.addClient(ip, host, port, "Connected");
                    }
                });
                System.out.println("Client connected: " + ip + " (" + host + ") on port " + port);
            }
        } catch (Exception e) {
            System.out.println("Error during client connection: " + e.getMessage());
        }
    }

    /**
     * 
     * @param iNetAddress
     */
    /**
     * updateClientAsDisconnected method.
     * @param iNetAddress the iNetAddress
     */
    private void updateClientAsDisconnected(InetAddress iNetAddress) {
        try {
            if (iNetAddress != null) {
                String ip = iNetAddress.getHostAddress();
                String host = iNetAddress.getHostName();
                ServerUi.updateClientStatus(ip, host, "Disconnected");
            }
        } catch (Exception e) {
            System.out.println("Error marking client disconnected: " + e.getMessage());
        }
    }

    @Override
    /**
     * serverStarted method.
     */
    protected void serverStarted() {
        System.out.println("Server listening for connections on port " + getPort());
    }

    @Override
    /**
     * serverStopped method.
     */
    protected void serverStopped() {
        System.out.println("Server has stopped listening for connections.");
        scheduler.shutdownNow();
    }

    /**
     * main method.
     * @param args the args
     */
    public static void main(String[] args) {
        int port = 5555;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (Throwable t) {
                System.out.println("Invalid port number. Using default 5555.");
            }
        }
        System.out.println("Open server on port " + port);
        EchoServer server = new EchoServer(port);
        try {
            server.listen();
        } catch (Exception ex) {
            System.out.println("ERROR - Could not listen for clients on port " + port);
        }
    }

    /**
     *
     */
    @Override
    /**
     * handleMessageFromClient method.
     * @param msg the msg
     * @param client the client
     */
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
        System.out.println("Message received: " + msg + " from " + client);

        if (!(msg instanceof String)) {
            System.err.println("Received non-string message: " + msg.getClass().getName());
            return;
        }

        String message = (String) msg;
        String[] parts = message.split("[:;]", 2);
        String command = parts[0].toUpperCase();
        String payload = (parts.length > 1) ? parts[1] : "";

        switch (command) {
            case "LOGIN":
                handleLogin(payload, client);
                break;
            case "SHOW_SLOTS":
                handleShowSlots(client);
                break;
            case "PARK_REQUEST":
                handleParkRequest(client);
                break;
            case "PARK_WITH_RESERVATION":
                handleParkWithReservation(payload, client);
                break;
            case "RELEASE_VEHICLE":
                handleReleaseVehicle(payload, client); // REFACTORED
                break;
            case "EXTEND_PARKING":
                handleExtendParking(payload, client); // REFACTORED
                break;
            case "FORGOT_CONFIRMATION_CODE":
                handleForgotCode(client);
                break;
            case "FUTURE_PARK_REQUEST":
                handleFutureParkRequest(payload, client);
                break;
            case "GET_FUTURE_SLOTS":
                handleGetFutureSlots(payload, client);
                break;
            case "UPDATE_SUBSCRIBER_INFO":
                handleUpdateSubscriber(payload, client);
                break;
            case "REGISTER_SUBSCRIBER":
                handleRegisterSubscriber(payload);
                break;
            case "GET_HISTORY":
                handleGetHistory(payload, client);
                break;
            case "CLIENT_DISCONNECTED":
                handleClientDisconnectMessage(client);
                break;
            case "GET_MAX_EXTENSION": // NEW
                handleGetMaxExtension(client);
                break;
            // Manager/Staff specific commands
            case "GET_ALL_ORDERS":
                handleGetAllOrders(client);
                break;
            case "GET_ALL_SCHEDULED_ORDERS":
                handleGetAllScheduledOrders(client);
                break;
            case "GET_ALL_SUBSCRIBERS":
                handleGetAllSubscribers(client);
                break;
            case "GET_ALL_ACTIVITY_LOGS":
                handleGetAllActivityLogs(client);
                break;
            case "SET_FREEZE_STATUS":
                handleSetFreezeStatus(payload);
                break;
            case "GET_MONTHLY_REPORT":
                handleGetMonthlyReport(payload, client);
                break;
            case "GET_DAILY_LATENESS_REPORT":
                handleGetDailyLatenessReport(payload, client);
                break;
            case "GET_SUBSCRIBER_PARKING_REPORT":
                handleGetSubscriberParkingReport(payload, client);
                break;
            case "GET_SLOT_OCCUPANCY_REPORT":
                handleGetSlotOccupancyReport(payload, client);
                break;
            default:
                System.err.println("Unknown command received: " + parts[0]);
                break;
        }
    }

    // --- Helper Methods for Handling Client Messages ---

    /**
     * handleLogin method.
     * @param payload the payload
     * @param client the client
     */
    private void handleLogin(String payload, ConnectionToClient client) {
        String subscriptionId = payload.trim();
        System.out.println("Trying to login with subscriptionId: " + subscriptionId);
        SubscriberInfo info = DBController.findSubscriberByCode(subscriptionId);
        try {
            if (info != null) {
                if (info.isFrozen()) {
                    db.logActivity(info.getSubscriptionCode(), "LOGIN_ATTEMPT_FROZEN", "Login denied, account is frozen.");
                    client.sendToClient("LOGIN_FAILED:Your account is frozen. Please contact customer support.");
                } else {
                    loggedInSubscribers.put(client, info);
                    db.logActivity(info.getSubscriptionCode(), "LOGIN", "Logged in successfully");
                    client.sendToClient("LOGIN_SUCCESS");
                    client.sendToClient(info);
                }
            } else {
                client.sendToClient("LOGIN_FAILED: Subscriber not found.");
            }
        } catch (IOException e) {
            System.err.println("Error sending login response: " + e.getMessage());
        }
    }

    /**
     * handleShowSlots method.
     * @param client the client
     */
    private void handleShowSlots(ConnectionToClient client) {
        SubscriberInfo subInfo = loggedInSubscribers.get(client);
        if (subInfo != null) {
            db.logActivity(subInfo.getSubscriptionCode(), "VIEW_SLOTS", "Viewed parking lot status");
        }
        ArrayList<Integer> occupied = DBController.getOccupiedSlots();
        try {
            client.sendToClient("show_slots");
            client.sendToClient(occupied);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * handleParkRequest method.
     * @param client the client
     */
    private void handleParkRequest(ConnectionToClient client) {
        SubscriberInfo subInfo = getLoggedInSubscriber(client, "PARK");
        if (subInfo == null) return;

        OrderInfo existingOrder = DBController.getOrderBySubscriberId(subInfo.getSubscriptionCode());
        if (existingOrder != null) {
            try {
                client.sendToClient("PARK_FAILED:You already have an active parking session.");
                db.logActivity(subInfo.getSubscriptionCode(), "PARK_ATTEMPT_DENIED", "Denied: Already has an active session in slot " + existingOrder.getParkingSpace());
            } catch (IOException e) { /* ignore */ }
            return;
        }

        ArrayList<Integer> occupied = DBController.getOccupiedSlots();
        int totalSlots = 100;
        ArrayList<Integer> allSlots = new ArrayList<>();
        for (int i = 1; i <= totalSlots; i++) allSlots.add(i);

        allSlots.removeAll(occupied);
        if (allSlots.isEmpty()) {
            try {
                client.sendToClient("PARK_FULL");
            } catch (IOException e) {
                System.out.println("Error sending PARK_FULL: " + e.getMessage());
            }
        } else {
            int assignedSlot = allSlots.get(new java.util.Random().nextInt(allSlots.size()));
            String confirmationCode = generateRandomCode(6);
            db.registerParkingSlot(assignedSlot, confirmationCode, subInfo.getSubscriptionCode());
            db.logActivity(subInfo.getSubscriptionCode(), "PARK_CAR", "Parked in slot " + assignedSlot + ". Code: " + confirmationCode);
            try {
                client.sendToClient("PARK_CONFIRMED:" + assignedSlot + ":" + confirmationCode);
            } catch (IOException e) {
                System.out.println("Error sending PARK_CONFIRMED: " + e.getMessage());
            }
        }
    }

    /**
     * handleParkWithReservation method.
     * @param payload the payload
     * @param client the client
     */
    private void handleParkWithReservation(String payload, ConnectionToClient client) {
        SubscriberInfo subInfo = getLoggedInSubscriber(client, "PARK_WITH_RESERVATION");
        if (subInfo == null) return;
        
        String code = payload.trim();
        int resultSlot = db.parkWithReservation(code, subInfo.getSubscriptionCode());
        try {
            if (resultSlot > 0) {
                client.sendToClient("PARK_CONFIRMED:" + resultSlot + ":" + code);
            } else if (resultSlot == -2) {
                client.sendToClient("PARK_WITH_RESERVATION_FAILED;It is too early to park. Please come back closer to your reservation time.");
            } else {
                client.sendToClient("PARK_WITH_RESERVATION_FAILED;Invalid reservation code or it does not belong to you.");
            }
        } catch (IOException e) {
            System.err.println("Error sending park with reservation response: " + e.getMessage());
        }
    }
    
    /**
    * REFACTORED: Now performs validation before extending and contains all lateness logic.
    */
    /**
     * handleReleaseVehicle method.
     * @param payload the payload
     * @param client the client
     */
    private void handleReleaseVehicle(String payload, ConnectionToClient client) {
        SubscriberInfo requestingSub = getLoggedInSubscriber(client, "RELEASE");
        if (requestingSub == null) return;
        
        String code = payload.trim();
        OrderInfo orderToRelease = db.getOrderByConfirmationCode(code);

        try {
            if (orderToRelease == null || orderToRelease.getParkingSpace() == null) {
                client.sendToClient("RELEASE_FAILED: Invalid or Incomplete Code");
                return;
            }
            if (!requestingSub.getSubscriptionCode().equals(orderToRelease.getSubscriberId())) {
                db.logActivity(requestingSub.getSubscriptionCode(), "RELEASE_ATTEMPT_DENIED", "Attempted to release vehicle with code " + code);
                client.sendToClient("RELEASE_FAILED: This confirmation code does not belong to your account.");
                return;
            }

            // Lateness Check
            Timestamp endParkTime = Timestamp.valueOf(orderToRelease.getEndParkTime());
            boolean isLate = LocalDateTime.now().isAfter(endParkTime.toLocalDateTime());

            boolean success = db.deleteOrderByConfirmationCode(code);

            if (success) {
                if (isLate) {
                    db.logActivity(orderToRelease.getSubscriberId(), "LATE_CAR_RETRIEVAL", "Car was taken late from slot " + orderToRelease.getParkingSpace());
                    db.incrementLateCountAndFreeze(orderToRelease.getSubscriberId());
                    
                    // Send appropriate email
                    SubscriberInfo subInfoForEmail = DBController.findSubscriberByCode(orderToRelease.getSubscriberId());
                    if (subInfoForEmail != null) {
                        if (subInfoForEmail.isFrozen()) {
                            EmailService.sendAccountFrozenEmail(subInfoForEmail.getEmail(), subInfoForEmail.getUserName(), subInfoForEmail.getLateCount());
                        } else {
                            EmailService.sendLateRetrievalEmail(subInfoForEmail.getEmail(), subInfoForEmail.getUserName());
                        }
                    }
                } else {
                    db.logActivity(orderToRelease.getSubscriberId(), "RELEASE_VEHICLE", "Released vehicle from slot " + orderToRelease.getParkingSpace());
                }
                client.sendToClient("RELEASE_SUCCESS:" + orderToRelease.getParkingSpace());
            } else {
                client.sendToClient("RELEASE_FAILED: Database error during deletion.");
            }
        } catch (Exception e) {
            System.err.println("Error sending release response: " + e.getMessage());
        }
    }

    /**
    * REFACTORED: Validates the extension request against the new business rules.
    */
    /**
     * handleExtendParking method.
     * @param payload the payload
     * @param client the client
     */
    private void handleExtendParking(String payload, ConnectionToClient client) {
        SubscriberInfo subInfo = getLoggedInSubscriber(client, "EXTEND");
        if (subInfo == null) return;
        
        try {
            // NEW: First, check if the user is already late.
            OrderInfo currentOrder = DBController.getOrderBySubscriberId(subInfo.getSubscriptionCode());
            if (currentOrder != null && currentOrder.getEndParkTime() != null) {
                Timestamp endParkTime = Timestamp.valueOf(currentOrder.getEndParkTime());
                if (LocalDateTime.now().isAfter(endParkTime.toLocalDateTime())) {
                    client.sendToClient("EXTEND_FAILED;Cannot extend, your parking session has expired.");
                    db.logActivity(subInfo.getSubscriptionCode(), "EXTEND_ATTEMPT_DENIED", "Denied: Parking session already expired.");
                    return;
                }
            } else if (currentOrder == null) {
                 // Be more explicit if the user has no active session to extend.
                 client.sendToClient("EXTEND_FAILED;Could not extend parking. You may not have an active session.");
                 return;
            }

            int hoursToExtend = Integer.parseInt(payload.trim());
            
            // Server-side validation for other constraints
            int maxAllowedHours = db.getMaximumAllowedExtension(subInfo.getSubscriptionCode());
            
            if (hoursToExtend <= 0) {
                 client.sendToClient("EXTEND_FAILED;Invalid number of hours.");
                 return;
            }

            if (hoursToExtend > maxAllowedHours) {
                String reason = (maxAllowedHours > 0) ? "You can extend by a maximum of " + maxAllowedHours + " hour(s)." : "Extension is not possible at this time (e.g., due to a future reservation).";
                client.sendToClient("EXTEND_FAILED;" + reason);
                return;
            }

            // If all validation passes, proceed with extension
            boolean success = db.extendParkingTime(subInfo.getSubscriptionCode(), hoursToExtend);
            if (success) {
                db.logActivity(subInfo.getSubscriptionCode(), "EXTEND_PARKING", "Extended parking by " + hoursToExtend + " hours.");
                client.sendToClient("EXTEND_SUCCESS;Your parking has been extended by " + hoursToExtend + " hours.");
            } else {
                // This is now a fallback message, as we checked for an active session earlier.
                client.sendToClient("EXTEND_FAILED;Could not extend parking due to an unexpected error.");
            }
        } catch (Exception e) {
            System.err.println("Error processing extend parking request: " + e.getMessage());
            try { client.sendToClient("EXTEND_FAILED;An internal server error occurred."); } catch (IOException ignored) {}
        }
    }

    /**
     * handleForgotCode method.
     * @param client the client
     */
    private void handleForgotCode(ConnectionToClient client) {
        SubscriberInfo subInfo = getLoggedInSubscriber(client, "FORGOT_CODE");
        if (subInfo == null) return;

        OrderInfo activeOrder = DBController.getOrderBySubscriberId(subInfo.getSubscriptionCode());
        if (activeOrder != null && activeOrder.getConfirmationCode() != null && !activeOrder.getConfirmationCode().isEmpty()) {
            EmailService.sendConfirmationCodeEmail(subInfo.getEmail(), activeOrder.getConfirmationCode(), subInfo.getUserName());
            db.logActivity(subInfo.getSubscriptionCode(), "FORGOT_CODE", "Requested confirmation code reminder via email.");
            try {
                client.sendToClient("FORGOT_CODE_SUCCESS");
            } catch (IOException e) {
                System.err.println("Failed to send FORGOT_CODE_SUCCESS to client: " + e.getMessage());
            }
        } else {
            try {
                client.sendToClient("FORGOT_CODE_FAILED:No active parking order found for your account.");
            } catch (IOException e) {
                System.err.println("Failed to send FORGOT_CODE_FAILED to client: " + e.getMessage());
            }
        }
    }

    /**
     * handleFutureParkRequest method.
     * @param payload the payload
     * @param client the client
     */
    private void handleFutureParkRequest(String payload, ConnectionToClient client) {
        SubscriberInfo subInfo = getLoggedInSubscriber(client, "FUTURE_PARK");
        if (subInfo == null) return;

        String[] parts = payload.split(";");
        if (parts.length != 2) {
            try { client.sendToClient("FUTURE_PARK_FAILED;Invalid request format."); } catch (IOException e) {}
            return;
        }

        try {
            int scheduledCount = db.countScheduledOrders();
            int futureCapacityLimit = 40; // 40% of 100 slots
            if (scheduledCount >= futureCapacityLimit) {
                client.sendToClient("FUTURE_PARK_FAILED;Future reservations are currently full. Please try parking upon arrival.");
                db.logActivity(subInfo.getSubscriptionCode(), "FUTURE_PARK_FAIL", "Booking failed, future reservation capacity is full.");
                return;
            }

            String dateTimeStr = parts[0] + " " + parts[1];
            ZoneId clientInputZone = ZoneId.of("Asia/Jerusalem");
            LocalDateTime ldt = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            ZonedDateTime clientZonedDateTime = ZonedDateTime.of(ldt, clientInputZone);
            ZonedDateTime nowInClientZone = ZonedDateTime.now(clientInputZone);

            if (clientZonedDateTime.isBefore(nowInClientZone.plus(24, ChronoUnit.HOURS))) {
                client.sendToClient("FUTURE_PARK_FAILED;Future booking must be at least 24 hours from now.");
                db.logActivity(subInfo.getSubscriptionCode(), "FUTURE_PARK_FAIL", "Booking failed, less than 24 hours notice.");
                return;
            }
            if (clientZonedDateTime.isAfter(nowInClientZone.plus(7, ChronoUnit.DAYS))) {
                client.sendToClient("FUTURE_PARK_FAILED;Future booking cannot be more than 7 days from now.");
                db.logActivity(subInfo.getSubscriptionCode(), "FUTURE_PARK_FAIL", "Booking failed, more than 7 days in advance.");
                return;
            }
            
            Timestamp scheduledTimestamp = Timestamp.from(clientZonedDateTime.toInstant());
            int availableSlot = db.findAvailableFutureSlot(scheduledTimestamp);

            if (availableSlot == -1) {
                client.sendToClient("FUTURE_PARK_FAILED;Parking lot is fully booked for the selected time. Please try another time.");
                db.logActivity(subInfo.getSubscriptionCode(), "FUTURE_PARK_FAIL", "Booking failed, lot full for " + dateTimeStr);
            } else {
                String confirmationCode = generateRandomCode(8);
                boolean success = db.scheduleParking(subInfo.getSubscriptionCode(), scheduledTimestamp, availableSlot, confirmationCode);
                if (success) {
                    EmailService.sendFutureConfirmationEmail(subInfo.getEmail(), subInfo.getUserName(), confirmationCode, dateTimeStr);
                    db.logActivity(subInfo.getSubscriptionCode(), "FUTURE_PARK_SUCCESS", "Booked parking for " + dateTimeStr + ". Spot: " + availableSlot + ". Code: " + confirmationCode);
                    client.sendToClient("FUTURE_PARK_SUCCESS;" + confirmationCode);
                } else {
                    client.sendToClient("FUTURE_PARK_FAILED;A database error occurred while saving the order.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing future park request: " + e.getMessage());
            e.printStackTrace();
            try { client.sendToClient("FUTURE_PARK_FAILED;An internal server error occurred."); } catch (IOException ignored) {}
        }
    }

    /**
     * handleGetFutureSlots method.
     * @param payload the payload
     * @param client the client
     */
    private void handleGetFutureSlots(String payload, ConnectionToClient client) {
        String date = payload.trim();
        try {
            ArrayList<Integer> futureSlots = db.getOccupiedSlotsForDate(date);
            client.sendToClient(futureSlots);
        } catch (IOException e) {
            System.err.println("Failed to send future slot data to client: " + e.getMessage());
        }
    }
    
    /**
     * handleUpdateSubscriber method.
     * @param payload the payload
     * @param client the client
     */
    private void handleUpdateSubscriber(String payload, ConnectionToClient client) {
        String[] parts = payload.split(";");
        if (parts.length == 4) {
            String code = parts[0];
            String name = parts[1];
            String phone = parts[2];
            String email = parts[3];
            
            System.out.println("Updating subscriber: " + code);
            boolean success = db.updateSubscriberInfo(code, name, phone, email);
            
            try {
                if (success) {
                    db.logActivity(code, "UPDATE_INFO", "Updated personal details.");
                    SubscriberInfo updatedInfo = DBController.findSubscriberByCode(code);
                    if (updatedInfo != null) {
                        client.sendToClient("UPDATE_SUCCESS");
                        client.sendToClient(updatedInfo);
                    } else {
                        client.sendToClient("UPDATE_FAILED: Could not retrieve updated record.");
                    }
                } else {
                    client.sendToClient("UPDATE_FAILED: Database update failed.");
                }
            } catch (IOException e) {
                System.out.println("Error sending update response: " + e.getMessage());
            }
        } else {
            System.err.println("Malformed UPDATE_SUBSCRIBER_INFO message: " + payload);
        }
    }

    /**
     * handleRegisterSubscriber method.
     * @param payload the payload
     */
    private void handleRegisterSubscriber(String payload) {
        String[] parts = payload.split(";");
        if (parts.length == 5) {
            String code = parts[0], name = parts[1], phone = parts[2], email = parts[3], id = parts[4];
            System.out.println("Registering subscriber: " + name + ", " + email);
            DBController.insertSubscriber(code, name, phone, email, id);
        } else {
            System.err.println("Malformed REGISTER_SUBSCRIBER message: " + payload);
        }
    }

    /**
     * handleGetHistory method.
     * @param payload the payload
     * @param client the client
     */
    private void handleGetHistory(String payload, ConnectionToClient client) {
        String subCode = payload.trim();
        try {
            ArrayList<ActivityInfo> history = db.getHistoryForSubscriber(subCode);
            client.sendToClient(history);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * handleClientDisconnectMessage method.
     * @param client the client
     */
    private void handleClientDisconnectMessage(ConnectionToClient client) {
        System.out.println("Client " + client.getInetAddress() + " reported disconnection.");
        loggedInSubscribers.remove(client);
        updateClientAsDisconnected(client.getInetAddress());
    }

    /**
    * NEW: Handles the client's request to get the maximum possible extension time.
    */
    /**
     * handleGetMaxExtension method.
     * @param client the client
     */
    private void handleGetMaxExtension(ConnectionToClient client) {
        SubscriberInfo subInfo = getLoggedInSubscriber(client, "GET_MAX_EXTENSION");
        if (subInfo == null) return;

        int maxHours = db.getMaximumAllowedExtension(subInfo.getSubscriptionCode());
        try {
            client.sendToClient("MAX_EXTENSION_RESULT:" + maxHours);
        } catch (IOException e) {
            System.err.println("Error sending max extension result: " + e.getMessage());
        }
    }

    // --- Manager/Staff Command Handlers ---

    /**
     * handleGetAllOrders method.
     * @param client the client
     */
    private void handleGetAllOrders(ConnectionToClient client) {
        try {
            client.sendToClient(db.getAllOrders());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * handleGetAllScheduledOrders method.
     * @param client the client
     */
    private void handleGetAllScheduledOrders(ConnectionToClient client) {
        try {
            client.sendToClient(db.getAllScheduledOrders());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * handleGetAllSubscribers method.
     * @param client the client
     */
    private void handleGetAllSubscribers(ConnectionToClient client) {
        try {
            client.sendToClient(db.getAllSubscribers());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * handleGetAllActivityLogs method.
     * @param client the client
     */
    private void handleGetAllActivityLogs(ConnectionToClient client) {
        try {
            ArrayList<ActivityInfo> allActivities = db.getAllActivities();
            client.sendToClient(allActivities);
        } catch (IOException e) {
            System.err.println("Failed to send all activity logs to manager: " + e.getMessage());
        }
    }
    
    /**
     * handleSetFreezeStatus method.
     * @param payload the payload
     */
    private void handleSetFreezeStatus(String payload) {
        try {
            String[] parts = payload.split(";");
            String subscriberId = parts[0];
            boolean freeze = "1".equals(parts[1]);
            
            if (db.setFreezeStatus(subscriberId, freeze)) {
                String activityType = freeze ? "ACCOUNT_MANUALLY_FROZEN" : "ACCOUNT_MANUALLY_UNFROZEN";
                db.logActivity(subscriberId, activityType, "Account status changed by a staff member.");
                System.out.println("Successfully set freeze status for " + subscriberId + " to " + freeze);
            } else {
                System.out.println("Failed to set freeze status for " + subscriberId);
            }
        } catch (Exception e) {
            System.err.println("Error processing SET_FREEZE_STATUS: " + e.getMessage());
        }
    }
    
    /**
     * handleGetMonthlyReport method.
     * @param payload the payload
     * @param client the client
     */
    private void handleGetMonthlyReport(String payload, ConnectionToClient client) {
        String[] parts = payload.split(";");
        if (parts.length == 2) {
            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                ArrayList<MonthlyReportData> reportData = db.getMonthlyParkingReport(year, month);
                client.sendToClient(reportData);
            } catch (NumberFormatException e) {
                System.err.println("Invalid year/month format in report request: " + payload);
            } catch (IOException e) {
                System.err.println("Failed to send monthly report to client: " + e.getMessage());
            }
        }
    }

    /**
     * handleGetDailyLatenessReport method.
     * @param payload the payload
     * @param client the client
     */
    private void handleGetDailyLatenessReport(String payload, ConnectionToClient client) {
        String[] parts = payload.split(";");
        if (parts.length == 2) {
            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                ArrayList<DailyLateData> reportData = db.getDailyLatenessReport(year, month);
                client.sendToClient(reportData);
            } catch (NumberFormatException e) {
                System.err.println("Invalid year/month format in lateness report request: " + payload);
            } catch (IOException e) {
                System.err.println("Failed to send daily lateness report to client: " + e.getMessage());
            }
        }
    }
    
    /**
     * handleGetSubscriberParkingReport method.
     * @param payload the payload
     * @param client the client
     */
    private void handleGetSubscriberParkingReport(String payload, ConnectionToClient client) {
        System.out.println("Received request for subscriber parking report.");
        String[] parts = payload.split(";");
        if (parts.length == 2) {
            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                ArrayList<SubscriberParkingData> reportData = db.getTotalParkingHoursPerSubscriber(year, month);
                client.sendToClient(reportData);
                System.out.println("Sent subscriber parking report to client for " + year + "-" + month);
            } catch (Exception e) {
                System.err.println("Failed to process subscriber parking report request: " + e.getMessage());
            }
        }
    }

    /**
     * handleGetSlotOccupancyReport method.
     * @param payload the payload
     * @param client the client
     */
    private void handleGetSlotOccupancyReport(String payload, ConnectionToClient client) {
        System.out.println("Received request for slot occupancy report.");
        String[] parts = payload.split(";");
        if (parts.length == 2) {
            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                ArrayList<SlotOccupancyData> reportData = db.getTotalParkingHoursPerSlot(year, month);
                client.sendToClient(reportData);
                System.out.println("Sent slot occupancy report to client for " + year + "-" + month);
            } catch (Exception e) {
                System.err.println("Failed to send slot occupancy report: " + e.getMessage());
            }
        }
    }

    // --- General Utility Methods ---

    /**
     * getLoggedInSubscriber method.
     * @param client the client
     * @param commandForErrorMessage the commandForErrorMessage
     * @return SubscriberInfo
     */
    private SubscriberInfo getLoggedInSubscriber(ConnectionToClient client, String commandForErrorMessage) {
        SubscriberInfo subInfo = loggedInSubscribers.get(client);
        if (subInfo == null) {
            try {
                client.sendToClient(commandForErrorMessage.toUpperCase() + "_FAILED;You are not logged in.");
            } catch (IOException e) { /* Ignore */ }
        }
        return subInfo;
    }

    /**
     * generateRandomCode method.
     * @param length the length
     * @return String
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return code.toString();
    }
    
    /**
     * gracefulShutdown method.
     * closes the server and send an termenating command for each connected client
     */
    public void gracefulShutdown() {
        try {
            for (Thread clientThread : getClientConnections()) {
                ConnectionToClient client = (ConnectionToClient) clientThread;
                try {
                    client.sendToClient("SHUTDOWN");
                } catch (Exception e) {
                    System.out.println("Failed to notify client: " + e.getMessage());
                }
            }
            Thread.sleep(200);
            close();
            System.out.println("Server shut down gracefully.");
        } catch (Exception e) {
            System.out.println("Error during shutdown: " + e.getMessage());
        }
    }
}