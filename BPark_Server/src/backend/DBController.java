package backend;

import common.DailyLateData;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import common.ActivityInfo;
import common.MonthlyReportData;
import common.OrderInfo;
import common.SlotOccupancyData;
import common.SubscriberInfo;
import common.SubscriberParkingData;

/**
 * Manages all interactions with the MySQL database for the BPark system.
 * This class includes methods for managing subscribers, parking orders (active and future),
 * activity logging, and generating reports.
 */
public class DBController {
    private static Connection currentConnection;

    private static final ExecutorService taskExecutor = Executors.newCachedThreadPool();

    /**
     * Establishes a connection to the 'bpark' database.
     * It loads the MySQL JDBC driver and attempts to connect to the local database instance.
     */
    public DBController() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            System.out.println("Driver definition succeed");
        } catch (Exception ex) {
            System.out.println("Driver definition failed");
        }

        try {
            currentConnection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bpark?serverTimezone=Asia/Jerusalem",
                "root",
                "Aa123456"
            );
            System.out.println("SQL connection succeed");
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    /**
     * Retrieves an active parking order for a specific subscriber.
     * This ensures a subscriber cannot have more than one active parking session at a time.
     *
     * @param subscriberId The ID of the subscriber.
     * @return An {@link OrderInfo} object if an active order exists, otherwise null.
     */
    public synchronized OrderInfo getActiveOrderBySubscriberCode(String subscriberId) {
        String sql = "SELECT * FROM activeparking WHERE subscriberId = ?";
        try (PreparedStatement pstmt = currentConnection.prepareStatement(sql)) {
            pstmt.setString(1, subscriberId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new OrderInfo(
                	rs.getString("parkingSpace"),
                    rs.getString("orderNumber"),
                    rs.getString("parkingSpace"),
                    rs.getString("subscriberId"),
                    rs.getString("confirmationCode"),
                    rs.getString("timeOfPlacingOrder"),
                    rs.getString("endParkTime")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calculates the maximum number of hours a subscriber can extend their current parking session.
     * The calculation is based on two constraints: the total parking time cannot exceed 8 hours,
     * and the extension cannot conflict with a future reservation for the same slot.
     *
     * @param subscriberId The ID of the subscriber requesting the extension.
     * @return The maximum allowed extension in hours. Returns 0 if no extension is possible.
     */
    public int getMaximumAllowedExtension(String subscriberId) {
        OrderInfo order = getOrderBySubscriberId(subscriberId);
        if (order == null || order.getParkingSpace() == null || order.getTimeOfPlacingOrder() == null || order.getEndParkTime() == null) {
            return 0;
        }

        try {
            int parkingSpace = Integer.parseInt(order.getParkingSpace());
            Timestamp timeOfPlacingOrder = Timestamp.valueOf(order.getTimeOfPlacingOrder());
            Timestamp currentEndParkTime = Timestamp.valueOf(order.getEndParkTime());

            LocalDateTime initialTime = timeOfPlacingOrder.toLocalDateTime();
            LocalDateTime maxPossibleEndTime = initialTime.plusHours(8);
            long budgetInHours = ChronoUnit.HOURS.between(currentEndParkTime.toLocalDateTime(), maxPossibleEndTime);
            if (budgetInHours < 0) budgetInHours = 0;

            long hoursUntilReservation = Long.MAX_VALUE;
            String sql = "SELECT MIN(scheduled_time) as next_booking FROM parkingorders WHERE futureParkingSpot = ? AND scheduled_time > ?";
            try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
                stmt.setInt(1, parkingSpace);
                stmt.setTimestamp(2, currentEndParkTime);
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getTimestamp("next_booking") != null) {
                    Timestamp nextBookingTime = rs.getTimestamp("next_booking");
                    hoursUntilReservation = ChronoUnit.HOURS.between(currentEndParkTime.toLocalDateTime(), nextBookingTime.toLocalDateTime());
                }
            } catch (SQLException e) {
                System.err.println("Error checking future reservations for extension: " + e.getMessage());
                return 0;
            }

            return (int) Math.min(budgetInHours, hoursUntilReservation);
        } catch(Exception e) {
            System.err.println("Error calculating max extension: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Scans for scheduled reservations that were not claimed within 15 minutes of the scheduled time.
     * It cancels these reservations, logs the cancellation, and returns a list of the cancelled orders.
     *
     * @return A list of {@link OrderInfo} objects representing the cancelled reservations.
     */
    public synchronized List<OrderInfo> checkAndCancelLateReservations() {
        List<OrderInfo> cancelledOrders = new ArrayList<>();
        String selectSql = "SELECT po.subscriberId, po.confirmationCode, po.scheduledTime, s.userName, s.email " +
                           "FROM parkingorders po " +
                           "JOIN subscriber s ON po.subscriberId = s.id " +
                           "WHERE po.scheduledTime < NOW() - INTERVAL 15 MINUTE";

        try (Statement stmt = currentConnection.createStatement(); ResultSet rs = stmt.executeQuery(selectSql)) {
            while (rs.next()) {
                OrderInfo order = new OrderInfo(
                    rs.getString("subscriberId"),
                    rs.getString("userName"),
                    rs.getString("scheduledTime"),
                    null,
                    rs.getString("confirmationCode")
                );
                order.setUserEmailForEmail(rs.getString("email"));
                cancelledOrders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return cancelledOrders;
        }
        if (!cancelledOrders.isEmpty()) {
            String deleteSql = "DELETE FROM parkingorders WHERE confirmationCode = ?";
            try (PreparedStatement deletePstmt = currentConnection.prepareStatement(deleteSql)) {
            	currentConnection.setAutoCommit(false);
                for (OrderInfo order : cancelledOrders) {
                    deletePstmt.setString(1, order.getConfirmationCode());
                    deletePstmt.addBatch();
                    logActivity(order.getSubscriberId(), "Reservation Canceled", "Canceled due to no-show for code " + order.getConfirmationCode());
                    System.out.println("Canceling reservation for " + order.getUserName() + " (Code: " + order.getConfirmationCode() + ")");
                }
                deletePstmt.executeBatch();
                currentConnection.commit();
            } catch (SQLException e) {
                try {
                	currentConnection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            } finally {
                try {
                	currentConnection.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return cancelledOrders;
    }


    /**
     * Retrieves a list of all parking slots that are reserved for a specific date.
     *
     * @param date The date to check in 'YYYY-MM-DD' format.
     * @return An ArrayList of integers representing the occupied future parking slots.
     */
    public ArrayList<Integer> getOccupiedSlotsForDate(String date) {
        ArrayList<Integer> occupiedSlots = new ArrayList<>();
        String query = "SELECT futureParkingSpot FROM parkingorders WHERE DATE(scheduled_time) = ?";
        try (PreparedStatement ps = currentConnection.prepareStatement(query)) {
            ps.setString(1, date);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                occupiedSlots.add(rs.getInt("futureParkingSpot"));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getOccupiedSlotsForDate: " + e.getMessage());
        }
        return occupiedSlots;
    }

    /**
     * Extends the parking time for a subscriber's active session.
     *
     * @param subscriberId The ID of the subscriber.
     * @param hours The number of hours to extend the parking session by.
     * @return True if the update was successful, false otherwise.
     */
    public boolean extendParkingTime(String subscriberId, int hours) {
        OrderInfo order = getOrderBySubscriberId(subscriberId);
        if (order == null) {
            System.out.println("Cannot extend: No active order found for subscriber " + subscriberId);
            return false;
        }
        String sql = "UPDATE `activeparking` SET endParkTime = DATE_ADD(endParkTime, INTERVAL ? HOUR) WHERE subscriber_id = ?";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setInt(1, hours);
            stmt.setString(2, subscriberId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("DB Error extending parking time for subscriber " + subscriberId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds an available parking slot for a future reservation.
     * It checks for conflicts with both currently active parking sessions and other scheduled orders.
     *
     * @param startTime The desired start time for the parking reservation.
     * @return An available slot number, or -1 if no slots are available.
     */
    public int findAvailableFutureSlot(Timestamp startTime) {
        int DURATION_HOURS = 4;
        LocalDateTime startDateTime = startTime.toLocalDateTime();
        LocalDateTime endDateTime = startDateTime.plusHours(DURATION_HOURS);

        List<Integer> allSlots = IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());
        List<Integer> busySlots = new ArrayList<>();

        String activeParkingSql = "SELECT parking_space FROM activeparking WHERE ? < endParkTime AND ? > time_of_placing_an_order";
        try (PreparedStatement stmt = currentConnection.prepareStatement(activeParkingSql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDateTime));
            stmt.setTimestamp(2, Timestamp.valueOf(endDateTime));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                busySlots.add(rs.getInt("parking_space"));
            }
        } catch (SQLException e) {
            System.err.println("Error checking active parking for future slots: " + e.getMessage());
            return -1;
        }

        String scheduledOrdersSql = "SELECT futureParkingSpot FROM parkingorders WHERE ? < (scheduled_time + INTERVAL ? HOUR) AND ? > scheduled_time";
        try (PreparedStatement stmt = currentConnection.prepareStatement(scheduledOrdersSql)) {
            stmt.setTimestamp(1, startTime);
            stmt.setInt(2, DURATION_HOURS);
            stmt.setTimestamp(3, Timestamp.valueOf(endDateTime));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String slotStr = rs.getString("futureParkingSpot").replaceAll("[^0-9]", "");
                if (!slotStr.isEmpty()) {
                    busySlots.add(Integer.parseInt(slotStr));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking scheduled orders for future slots: " + e.getMessage());
            return -1;
        }

        allSlots.removeAll(busySlots);

        if (allSlots.isEmpty()) {
            return -1;
        }

        Collections.shuffle(allSlots);
        return allSlots.get(0);
    }

    /**
     * Inserts a new future parking order into the database.
     *
     * @param subscriberId     The ID of the subscriber making the reservation.
     * @param scheduledTime    The scheduled time for the parking.
     * @param slotNumber       The assigned parking slot number.
     * @param confirmationCode A unique code for the reservation.
     * @return True if the insertion was successful, false otherwise.
     */
    public boolean scheduleParking(String subscriberId, Timestamp scheduledTime, int slotNumber, String confirmationCode) {
        String sql = "INSERT INTO `parkingorders` (subscriptionCode, scheduled_time, futureParkingSpot, confirmationCode, reminder_sent) VALUES (?, ?, ?, ?, 0)";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setString(1, subscriberId);
            stmt.setTimestamp(2, scheduledTime);
            stmt.setInt(3, slotNumber);
            stmt.setString(4, confirmationCode);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                 System.err.println("Failed to schedule parking: Duplicate confirmation code generated. Please try again.");
            } else {
                 System.err.println("Failed to schedule parking: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Counts the total number of scheduled (future) parking orders.
     *
     * @return The number of scheduled orders, or 999 if an error occurs.
     */
    public int countScheduledOrders() {
        String sql = "SELECT COUNT(*) FROM parkingorders";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Failed to count scheduled orders: " + e.getMessage());
        }
        return 999;
    }

    /**
     * Retrieves a list of scheduled orders that are due for a reminder email.
     * An order needs a reminder if it is scheduled within the next 16 minutes and a reminder has not been sent yet.
     *
     * @return An ArrayList of {@link OrderInfo} objects for orders that need a reminder.
     */
    public ArrayList<OrderInfo> getOrdersForReminder() {
        ArrayList<OrderInfo> orders = new ArrayList<>();
        String sql = "SELECT po.subscriptionCode, po.scheduled_time, po.confirmationCode, s.userName, s.email " +
                     "FROM `parkingorders` po " +
                     "JOIN `subscriber` s ON po.subscriptionCode = s.subscriptionCode " +
                     "WHERE po.reminder_sent = 0 AND " +
                     "po.scheduled_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 16 MINUTE)";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Timestamp scheduledTimestamp = rs.getTimestamp("scheduled_time");
                String scheduledTimeStr = scheduledTimestamp != null ? scheduledTimestamp.toLocalDateTime().truncatedTo(ChronoUnit.MINUTES).toString().replace("T", " ") : "N/A";

                OrderInfo info = new OrderInfo(
                    rs.getString("subscriptionCode"),
                    rs.getString("userName"),
                    scheduledTimeStr,
                    null,
                    rs.getString("confirmationCode")
                );

                info.setUserNameForEmail(rs.getString("userName"));
                info.setUserEmailForEmail(rs.getString("email"));
                orders.add(info);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching orders for reminder: " + e.getMessage());
        }
        return orders;
    }

    /**
     * Marks a scheduled order's reminder as having been sent.
     *
     * @param confirmationCode The confirmation code of the order to update.
     */
    public void markReminderAsSent(String confirmationCode) {
        String sql = "UPDATE `parkingorders` SET reminder_sent = 1 WHERE confirmationCode = ?";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setString(1, confirmationCode);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to mark reminder as sent for code " + confirmationCode + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves a list of all scheduled future orders, sorted by the scheduled time.
     *
     * @return An ArrayList of {@link OrderInfo} objects representing all scheduled orders.
     */
    public ArrayList<OrderInfo> getAllScheduledOrders() {
        ArrayList<OrderInfo> orders = new ArrayList<>();
        String sql = "SELECT po.*, s.userName FROM `parkingorders` po JOIN `subscriber` s ON po.subscriptionCode = s.subscriptionCode ORDER BY po.scheduled_time ASC";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Timestamp scheduledTimestamp = rs.getTimestamp("scheduled_time");
                 String scheduledTimeStr = scheduledTimestamp != null ? scheduledTimestamp.toLocalDateTime().truncatedTo(ChronoUnit.MINUTES).toString().replace("T", " ") : "N/A";

                OrderInfo info = new OrderInfo(
                    rs.getString("subscriptionCode"),
                    rs.getString("userName"),
                    scheduledTimeStr,
                    rs.getString("futureParkingSpot"),
                    rs.getString("confirmationCode")
                );
                orders.add(info);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all scheduled orders: " + e.getMessage());
        }
        return orders;
    }

    /**
     * Retrieves the activity history for a specific subscriber, ordered by most recent first.
     *
     * @param subscriberCode The code of the subscriber.
     * @return An ArrayList of {@link ActivityInfo} objects representing the subscriber's history.
     */
    public ArrayList<ActivityInfo> getHistoryForSubscriber(String subscriberCode) {
        ArrayList<ActivityInfo> history = new ArrayList<>();
        String sql = "SELECT activity_type, details, DATE_FORMAT(activity_timestamp, '%Y-%m-%d %H:%i:%s') as formatted_timestamp FROM activity_log WHERE subscriber_code = ? ORDER BY activity_timestamp DESC";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setString(1, subscriberCode);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ActivityInfo log = new ActivityInfo(
                    rs.getString("activity_type"),
                    rs.getString("details"),
                    rs.getString("formatted_timestamp")
                );
                history.add(log);
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch history for subscriber " + subscriberCode + ": " + e.getMessage());
        }
        return history;
    }

    /**
     * Retrieves all activity logs from the database, ordered by most recent first.
     *
     * @return An ArrayList of {@link ActivityInfo} objects for all activities.
     */
    public ArrayList<ActivityInfo> getAllActivities() {
        ArrayList<ActivityInfo> history = new ArrayList<>();
        String sql = "SELECT al.subscriber_code, s.userName, al.activity_type, al.details, DATE_FORMAT(al.activity_timestamp, '%Y-%m-%d %H:%i:%s') as formatted_timestamp FROM activity_log al LEFT JOIN subscriber s ON al.subscriber_code = s.subscriptionCode ORDER BY al.activity_timestamp DESC";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ActivityInfo log = new ActivityInfo(
                    rs.getString("subscriber_code"),
                    rs.getString("userName"),
                    rs.getString("activity_type"),
                    rs.getString("details"),
                    rs.getString("formatted_timestamp")
                );
                history.add(log);
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch all activity logs: " + e.getMessage());
        }
        return history;
    }

    /**
     * Generates a monthly report of parking activities, showing the count of cars parked each day.
     *
     * @param year  The year of the report.
     * @param month The month of the report.
     * @return An ArrayList of {@link MonthlyReportData} objects.
     */
    public ArrayList<MonthlyReportData> getMonthlyParkingReport(int year, int month) {
        ArrayList<MonthlyReportData> reportData = new ArrayList<>();
        String sql = "SELECT DATE(activity_timestamp) as park_date, COUNT(*) as daily_count FROM activity_log WHERE activity_type = 'PARK_CAR' AND YEAR(activity_timestamp) = ? AND MONTH(activity_timestamp) = ? GROUP BY DATE(activity_timestamp) ORDER BY park_date ASC";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MonthlyReportData dailyData = new MonthlyReportData(
                    rs.getString("park_date"),
                    rs.getInt("daily_count")
                );
                reportData.add(dailyData);
            }
        } catch (SQLException e) {
            System.err.println("Failed to generate monthly parking report: " + e.getMessage());
        }
        return reportData;
    }

    /**
     * Logs an activity for a subscriber in the database.
     *
     * @param subscriberCode The code of the subscriber performing the activity.
     * @param activityType   The type of activity (e.g., 'LOGIN', 'PARK_CAR').
     * @param details        A detailed description of the activity.
     */
    public void logActivity(String subscriberCode, String activityType, String details) {
        String sql = "INSERT INTO activity_log (subscriber_code, activity_type, details) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)){
            stmt.setString(1, subscriberCode);
            stmt.setString(2, activityType);
            stmt.setString(3, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to log activity for subscriber " + subscriberCode + ": " + e.getMessage());
        }
    }

    /**
     * Inserts a new subscriber into the database and sends a welcome email.
     *
     * @param code  The subscriber's unique subscription code.
     * @param name  The subscriber's name.
     * @param phone The subscriber's phone number.
     * @param email The subscriber's email address.
     * @param id    The subscriber's national/personal ID.
     */
    public static void insertSubscriber(String code, String name, String phone, String email, String id) {
        try {
            String query = "INSERT INTO subscriber (subscriptionCode, userName, phoneNumber, email, id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = currentConnection.prepareStatement(query);
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, phone);
            ps.setString(4, email);
            ps.setString(5, id);
            ps.executeUpdate();
            ps.close();
            taskExecutor.submit(() -> EmailService.sendWelcomeEmail(email, code));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a list of all currently occupied parking slots.
     *
     * @return An ArrayList of integers representing the occupied slot numbers.
     */
    public static ArrayList<Integer> getOccupiedSlots() {
        ArrayList<Integer> occupiedSlots = new ArrayList<>();
        String query = "SELECT parking_space FROM activeparking WHERE parking_space IS NOT NULL";
        try (PreparedStatement ps = currentConnection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                occupiedSlots.add(rs.getInt("parking_space"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return occupiedSlots;
    }

    /**
     * Updates the information for an existing subscriber.
     *
     * @param subscriptionCode The code of the subscriber to update.
     * @param userName         The new user name.
     * @param phoneNumber      The new phone number.
     * @param email            The new email address.
     * @return True if the update was successful, false otherwise.
     */
    public boolean updateSubscriberInfo(String subscriptionCode, String userName, String phoneNumber, String email) {
        String sql = "UPDATE subscriber SET userName = ?, phoneNumber = ?, email = ? WHERE subscriptionCode = ?";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setString(1, userName);
            stmt.setString(2, phoneNumber);
            stmt.setString(3, email);
            stmt.setString(4, subscriptionCode);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating subscriber info: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the active parking order for a given subscriber ID.
     *
     * @param subscriberId The ID of the subscriber.
     * @return An {@link OrderInfo} object if an active order is found, otherwise null.
     */
    public static OrderInfo getOrderBySubscriberId(String subscriberId) {
        String sql = "SELECT *, DATE_FORMAT(time_of_placing_an_order, '%Y-%m-%d %H:%i:%s') as formatted_start, DATE_FORMAT(endParkTime, '%Y-%m-%d %H:%i:%s') as formatted_end FROM `activeparking` WHERE subscriber_id = ?";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setString(1, subscriberId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new OrderInfo(
                    rs.getString("parking_space"),
                    rs.getString("order_number"),
                    rs.getString("order_date"),
                    rs.getString("confirmation_code"),
                    rs.getString("subscriber_id"),
                    rs.getString("formatted_start"),
                    rs.getString("formatted_end")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves an active parking order by its confirmation code.
     *
     * @param confirmationCode The confirmation code of the order.
     * @return An {@link OrderInfo} object if found, otherwise null.
     */
    public OrderInfo getOrderByConfirmationCode(String confirmationCode) {
        String sql = "SELECT *, DATE_FORMAT(time_of_placing_an_order, '%Y-%m-%d %H:%i:%s') as formatted_start, DATE_FORMAT(endParkTime, '%Y-%m-%d %H:%i:%s') as formatted_end FROM `activeparking` WHERE confirmation_code = ? LIMIT 1";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setString(1, confirmationCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new OrderInfo(
                    rs.getString("parking_space"),
                    rs.getString("order_number"),
                    rs.getString("order_date"),
                    rs.getString("confirmation_code"),
                    rs.getString("subscriber_id"),
                    rs.getString("formatted_start"),
                    rs.getString("formatted_end")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Registers a new active parking session in the database.
     *
     * @param slot             The assigned parking slot number.
     * @param confirmationCode The unique confirmation code for this session.
     * @param subscriberId     The ID of the subscriber parking the car.
     */
    public void registerParkingSlot(int slot, String confirmationCode, String subscriberId) {
        String sql = "INSERT INTO `activeparking` (parking_space, confirmation_code, order_date, subscriber_id, time_of_placing_an_order) VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setInt(1, slot);
            stmt.setString(2, confirmationCode);
            stmt.setString(3, java.time.LocalDate.now().toString());
            stmt.setString(4, subscriberId);
            stmt.executeUpdate();
            System.out.println(" Registered parking slot " + slot + " for subscriber " + subscriberId + " with code " + confirmationCode);
        } catch (SQLException e) {
            System.out.println(" Failed to register parking slot: " + e.getMessage());
        }
    }

    /**
     * Deletes an active parking order from the database using its confirmation code.
     * This method is called when a vehicle is released from the parking lot.
     *
     * @param confirmationCode The confirmation code of the order to delete.
     * @return True if the deletion was successful, false otherwise.
     */
    public boolean deleteOrderByConfirmationCode(String confirmationCode) {
        String deleteSql = "DELETE FROM activeparking WHERE confirmation_code = ?";
        try (PreparedStatement deleteStmt = currentConnection.prepareStatement(deleteSql)) {
            deleteStmt.setString(1, confirmationCode);
            return deleteStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves a list of all currently active parking orders.
     *
     * @return An ArrayList of {@link OrderInfo} objects for all active orders.
     */
    public ArrayList<OrderInfo> getAllOrders() {
        ArrayList<OrderInfo> orders = new ArrayList<>();
        String sql = "SELECT *, DATE_FORMAT(time_of_placing_an_order, '%Y-%m-%d %H:%i:%s') as formatted_start, DATE_FORMAT(endParkTime, '%Y-%m-%d %H:%i:%s') as formatted_end FROM `activeparking`";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                OrderInfo info = new OrderInfo(
                    rs.getString("parking_space"),
                    rs.getString("order_number"),
                    rs.getString("order_date"),
                    rs.getString("confirmation_code"),
                    rs.getString("subscriber_id"),
                    rs.getString("formatted_start"),
                    rs.getString("formatted_end")
                );
                orders.add(info);
            }
        } catch (SQLException e) {
            System.out.println("Failed to fetch orders: " + e.getMessage());
        }
        return orders;
    }

    /**
     * Increments a subscriber's late count. If the late count reaches 2 or more,
     * the subscriber's account is frozen. This operation is performed within a transaction.
     *
     * @param subscriberId The ID of the subscriber who was late.
     */
    public void incrementLateCountAndFreeze(String subscriberId) {
        try {
            currentConnection.setAutoCommit(false);
            String incrementSql = "UPDATE subscriber SET timesLate = timesLate + 1 WHERE subscriptionCode = ?";
            try (PreparedStatement stmt = currentConnection.prepareStatement(incrementSql)) {
                stmt.setString(1, subscriberId);
                stmt.executeUpdate();
            }

            String selectSql = "SELECT timesLate FROM subscriber WHERE subscriptionCode = ?";
            int lateCount = 0;
            try (PreparedStatement stmt = currentConnection.prepareStatement(selectSql)) {
                stmt.setString(1, subscriberId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    lateCount = rs.getInt("timesLate");
                }
            }

            if (lateCount >= 2) {
                String freezeSql = "UPDATE subscriber SET isFrozen = 1 WHERE subscriptionCode = ?";
                try (PreparedStatement stmt = currentConnection.prepareStatement(freezeSql)) {
                    stmt.setString(1, subscriberId);
                    stmt.executeUpdate();
                    logActivity(subscriberId, "ACCOUNT_FROZEN", "Account frozen due to reaching " + lateCount + " late incidents.");
                }
            }

            currentConnection.commit();
        } catch (SQLException e) {
            System.err.println("Transaction failed in incrementLateCountAndFreeze: " + e.getMessage());
            try {
                currentConnection.rollback();
            } catch (SQLException ex) {
                System.err.println("Rollback failed: " + ex.getMessage());
            }
        } finally {
            try {
                currentConnection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    /**
     * Sets the freeze status of a subscriber's account.
     *
     * @param subscriberId The ID of the subscriber.
     * @param freeze       True to freeze the account, false to unfreeze.
     * @return True if the update was successful, false otherwise.
     */
    public boolean setFreezeStatus(String subscriberId, boolean freeze) {
        String sql = "UPDATE subscriber SET isFrozen = ? WHERE subscriptionCode = ?";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setInt(1, freeze ? 1 : 0);
            stmt.setString(2, subscriberId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DB Error setting freeze status for " + subscriberId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds a subscriber by their subscription code.
     *
     * @param subscriptionCode The code of the subscriber to find.
     * @return A {@link SubscriberInfo} object if found, otherwise null.
     */
    public static SubscriberInfo findSubscriberByCode(String subscriptionCode) {
        String sql = "SELECT * FROM subscriber WHERE subscriptionCode = ?";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setString(1, subscriptionCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new SubscriberInfo(
                    rs.getString("subscriptionCode"),
                    rs.getString("userName"),
                    rs.getString("phoneNumber"),
                    rs.getString("email"),
                    rs.getString("id"),
                    rs.getInt("timesLate"),
                    rs.getInt("isFrozen") == 1
                );
            }
        } catch (SQLException e) {
            System.out.println("Error finding subscriber by code: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a list of all subscribers from the database.
     *
     * @return An ArrayList of {@link SubscriberInfo} objects.
     */
    public ArrayList<SubscriberInfo> getAllSubscribers() {
        ArrayList<SubscriberInfo> subscribers = new ArrayList<>();
        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM subscriber");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                subscribers.add(new SubscriberInfo(
                    rs.getString("subscriptionCode"),
                    rs.getString("userName"),
                    rs.getString("phoneNumber"),
                    rs.getString("email"),
                    rs.getString("id"),
                    rs.getInt("timesLate"),
                    rs.getInt("isFrozen") == 1
                ));
            }
        } catch (SQLException e) {
            System.out.println("Failed to fetch subscribers: " + e.getMessage());
        }
        return subscribers;
    }

    /**
     * Checks if a specific parking slot is currently occupied.
     *
     * @param slotNumber The slot number to check.
     * @return True if the slot is occupied, false otherwise.
     */
    private synchronized boolean isSlotOccupied(int slotNumber) {
        String sql = "SELECT COUNT(*) FROM activeparking WHERE parkingSpace = ?";
        try (PreparedStatement pstmt = currentConnection.prepareStatement(sql)) {
            pstmt.setInt(1, slotNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Finds the next available parking slot number by checking which slots are not in the activeparking table.
     *
     * @return The number of the next available slot, or -1 if the lot is full or a database error occurs.
     */
    private synchronized int findNextAvailableSlot() {
        String sql = "SELECT parkingSpace FROM activeparking";
        List<Integer> occupiedSlots = new ArrayList<>();
        try (Statement stmt = currentConnection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                occupiedSlots.add(rs.getInt("parkingSpace"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }

        for (int i = 1; i <= 100; i++) {
            if (!occupiedSlots.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Manages the process of parking a car with a prior reservation.
     * It validates the reservation, checks for lateness, handles slot conflicts,
     * and updates the database tables within a transaction.
     *
     * @param confirmationCode The reservation confirmation code.
     * @param subscriberId     The ID of the subscriber.
     * @return The assigned parking slot number on success. Returns negative integers for specific errors:
     * -1 for a general DB error, -2 for arriving too early, -3 if no slots are available,
     * -4 for an invalid reservation code or mismatched subscriber.
     */
    public int parkWithReservation(String confirmationCode, String subscriberId) {
        String findOrderSql = "SELECT * FROM parkingorders WHERE confirmationCode = ? AND subscriptionCode = ?";
        try (PreparedStatement findStmt = currentConnection.prepareStatement(findOrderSql)) {
            findStmt.setString(1, confirmationCode);
            findStmt.setString(2, subscriberId);
            ResultSet rs = findStmt.executeQuery();

            if (!rs.next()) {
                return -4;
            }

            Timestamp scheduledTime = rs.getTimestamp("scheduled_time");
            int reservedSlot = rs.getInt("futureParkingSpot");

            if (LocalDateTime.now().isBefore(scheduledTime.toLocalDateTime().minusMinutes(1))) {
                return -2;
            }

            if (LocalDateTime.now().isAfter(scheduledTime.toLocalDateTime().plusMinutes(15))) {
                incrementLateCountAndFreeze(subscriberId);
            }

            if (isSlotOccupied(reservedSlot)) {
                int newSlot = findNextAvailableSlot();
                if (newSlot == -1) {
                    return -3;
                }
                reservedSlot = newSlot;
            }

            currentConnection.setAutoCommit(false);

            try {
                String deleteSql = "DELETE FROM parkingorders WHERE confirmationCode = ?";
                try (PreparedStatement deleteStmt = currentConnection.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, confirmationCode);
                    deleteStmt.executeUpdate();
                }

                String insertSql = "INSERT INTO activeparking (parking_space, confirmation_code, order_date, subscriber_id, time_of_placing_an_order, endParkTime) " +
                                   "VALUES (?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 4 HOUR))";
                try (PreparedStatement insertStmt = currentConnection.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, reservedSlot);
                    insertStmt.setString(2, confirmationCode);
                    insertStmt.setString(3, java.time.LocalDate.now().toString());
                    insertStmt.setString(4, subscriberId);
                    insertStmt.executeUpdate();
                }

                currentConnection.commit();
                logActivity(subscriberId, "PARK_WITH_RESERVATION", "Parked in slot " + reservedSlot + " with code " + confirmationCode);
                return reservedSlot;

            } catch (SQLException e) {
                currentConnection.rollback();
                System.err.println("Transaction failed in parkWithReservation: " + e.getMessage());
                return -1;
            } finally {
                currentConnection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("DB Error in parkWithReservation: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Calculates the total parking hours for each subscriber for a given month and year.
     *
     * @param year  The year of the report.
     * @param month The month of the report.
     * @return An ArrayList of {@link SubscriberParkingData} objects, sorted by total hours.
     */
    public ArrayList<SubscriberParkingData> getTotalParkingHoursPerSubscriber(int year, int month) {
        ArrayList<SubscriberParkingData> subscriberReports = new ArrayList<>();
        String query = "SELECT ap.subscriber_id, s.userName, SUM(TIMESTAMPDIFF(HOUR, ap.time_of_placing_an_order, ap.endParkTime)) AS total_parked_hours " +
                       "FROM activeparking ap JOIN subscriber s ON ap.subscriber_id = s.subscriptionCode " +
                       "WHERE YEAR(ap.time_of_placing_an_order) = ? AND MONTH(ap.time_of_placing_an_order) = ? " +
                       "GROUP BY ap.subscriber_id, s.userName ORDER BY total_parked_hours DESC";
        try (PreparedStatement stmt = currentConnection.prepareStatement(query)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String subscriberId = rs.getString("subscriber_id");
                String userName = rs.getString("userName");
                int totalHours = rs.getInt("total_parked_hours");
                subscriberReports.add(new SubscriberParkingData(subscriberId, userName, totalHours));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getTotalParkingHoursPerSubscriber: " + e.getMessage());
        }
        return subscriberReports;
    }

    /**
     * Calculates the total occupancy hours for each parking slot for a given month and year.
     *
     * @param year  The year of the report.
     * @param month The month of the report.
     * @return An ArrayList of {@link SlotOccupancyData} objects, sorted by total hours.
     */
    public ArrayList<SlotOccupancyData> getTotalParkingHoursPerSlot(int year, int month) {
        ArrayList<SlotOccupancyData> slotReports = new ArrayList<>();
        String query = "SELECT parking_space, SUM(TIMESTAMPDIFF(HOUR, time_of_placing_an_order, endParkTime)) AS total_occupied_hours " +
                       "FROM activeparking " +
                       "WHERE YEAR(time_of_placing_an_order) = ? AND MONTH(time_of_placing_an_order) = ? " +
                       "GROUP BY parking_space ORDER BY total_occupied_hours DESC";
        try (PreparedStatement stmt = currentConnection.prepareStatement(query)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String parkingSpace = rs.getString("parking_space");
                int totalHours = rs.getInt("total_occupied_hours");
                slotReports.add(new SlotOccupancyData(parkingSpace, totalHours));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getTotalParkingHoursPerSlot: " + e.getMessage());
        }
        return slotReports;
    }

    /**
     * Retrieves data for a daily lateness report for a specific month and year.
     * It counts the number of late car retrievals for each day.
     *
     * @param year  The year for the report.
     * @param month The month for the report.
     * @return An ArrayList of {@link DailyLateData} containing the number of late incidents per day.
     */
    public ArrayList<DailyLateData> getDailyLatenessReport(int year, int month) {
        ArrayList<DailyLateData> reportData = new ArrayList<>();
        String sql = "SELECT DATE(activity_timestamp) as late_date, COUNT(*) as daily_count FROM activity_log WHERE activity_type = 'LATE_CAR_RETRIEVAL' AND YEAR(activity_timestamp) = ? AND MONTH(activity_timestamp) = ? GROUP BY DATE(activity_timestamp) ORDER BY late_date ASC";
        try (PreparedStatement stmt = currentConnection.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
            	DailyLateData dailyData = new DailyLateData(
                    rs.getString("late_date"),
                    rs.getInt("daily_count")
                );
                reportData.add(dailyData);
            }
        } catch (SQLException e) {
            System.err.println("Failed to generate daily lateness report: " + e.getMessage());
        }
        return reportData;
    }
}