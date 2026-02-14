package backend;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

import common.OrderInfo;

/**
 * A utility class for sending various types of emails to subscribers.
 * It handles sending welcome emails, confirmation codes, reminders, and notifications.
 */
public class EmailService {
    final static String fromEmail = "braudepark@gmail.com";
    final static String password = "bdjfxtivxdwaaalg";

    /**
     * Sends a welcome email to a new subscriber.
     *
     * @param toEmail       The email address of the new subscriber.
     * @param subscriptionId The new subscriber's subscription ID.
     */
    public static void sendWelcomeEmail(String toEmail, String subscriptionId) {
        System.out.println("📧 ENTERED sendWelcomeEmail");
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Welcome to BPark!");
            message.setText("Dear New Subscriber,\n\nWelcome to BPark!\n\nYour new subscription ID is: " + subscriptionId + "\n\nYou can use this code to log in to the client application.\n\nThank you for joining us!\n\nThe BPark Team");
            Transport.send(message);
            System.out.println("Welcome email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send welcome email to " + toEmail);
            e.printStackTrace();
        }
    }

    /**
     * Sends an email with a confirmation code for vehicle retrieval.
     * This is typically used when a subscriber forgets their code.
     *
     * @param toEmail         The subscriber's email address.
     * @param confirmationCode The confirmation code for the active parking session.
     * @param userName        The subscriber's name.
     */
    public static void sendConfirmationCodeEmail(String toEmail, String confirmationCode, String userName) {
        System.out.println("📧 ENTERED sendConfirmationCodeEmail");
        Session session = createSession();
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("BPark - Your Vehicle Confirmation Code");
            message.setText("Dear " + userName + ",\n\nAs requested, here is your confirmation code to release your vehicle:\n\n"
                    + "Confirmation Code: " + confirmationCode + "\n\n"
                    + "Please use this code in the client application to take your car.\n\n"
                    + "The BPark Team");
            Transport.send(message);
            System.out.println("Confirmation code email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send confirmation code email to " + toEmail);
            e.printStackTrace();
        }
    }

    /**
     * Sends an email to confirm a future parking reservation.
     *
     * @param toEmail         The subscriber's email address.
     * @param userName        The subscriber's name.
     * @param confirmationCode The unique code for the future reservation.
     * @param scheduledTime   The time the reservation is scheduled for.
     */
    public static void sendFutureConfirmationEmail(String toEmail, String userName, String confirmationCode, String scheduledTime) {
        System.out.println("📧 ENTERED sendFutureConfirmationEmail");
        Session session = createSession();
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("BPark - Your Future Parking is Confirmed!");
            message.setText("Dear " + userName + ",\n\nYour future parking spot has been successfully reserved.\n\n"
                    + "Reservation Time: " + scheduledTime + "\n"
                    + "Confirmation Code: " + confirmationCode + "\n\n"
                    + "You will receive a reminder email 15 minutes before your scheduled time.\n"
                    + "Please use this confirmation code to manage your booking.\n\n"
                    + "The BPark Team");
            Transport.send(message);
            System.out.println("Future parking confirmation sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send future confirmation email to " + toEmail);
            e.printStackTrace();
        }
    }

    /**
     * Sends a reminder email for an upcoming parking reservation.
     * This is typically sent 15 minutes before the scheduled time.
     *
     * @param toEmail         The subscriber's email address.
     * @param userName        The subscriber's name.
     * @param confirmationCode The reservation's confirmation code.
     * @param scheduledTime   The reservation's scheduled time.
     */
    public static void sendReminderEmail(String toEmail, String userName, String confirmationCode, String scheduledTime) {
        System.out.println("📧 ENTERED sendReminderEmail");
        Session session = createSession();
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("BPark - Reminder: Your Upcoming Parking Reservation");
            message.setText("Dear " + userName + ",\n\nThis is a friendly reminder that your parking reservation is scheduled for:\n\n"
                    + "Time: " + scheduledTime + "\n"
                    + "Confirmation Code: " + confirmationCode + "\n\n"
                    + "Please make your way to the parking lot.\n\n"
                    + "The BPark Team");
            Transport.send(message);
            System.out.println("Reservation reminder sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send reminder email to " + toEmail);
            e.printStackTrace();
        }
    }

    /**
     * Sends an email notifying a subscriber that their reservation was cancelled
     * due to a no-show (not arriving within 15 minutes of the scheduled time).
     *
     * @param toEmail         The subscriber's email address.
     * @param userName        The subscriber's name.
     * @param confirmationCode The cancelled reservation's code.
     * @param scheduledTime   The cancelled reservation's scheduled time.
     */
    public static void sendLateReservationCancellationEmail(String toEmail, String userName, String confirmationCode, String scheduledTime) {
        System.out.println("📧 ENTERED sendLateReservationCancellationEmail");
        Session session = createSession();
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("BPark - Your Reservation Has Been Cancelled");
            message.setText("Dear " + userName + ",\n\nYour parking reservation for " + scheduledTime
                    + " with confirmation code " + confirmationCode
                    + " has been automatically cancelled because you did not arrive within 15 minutes of the scheduled time.\n\n"
                    + "This incident has been recorded in your account history, and your late count has been incremented accordingly.\n\n"
                    + "If you believe this is an error, please contact customer support.\n\n"
                    + "The BPark Team");
            Transport.send(message);
            System.out.println("Late reservation cancellation email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send late cancellation email to " + toEmail);
            e.printStackTrace();
        }
    }

    /**
     * Creates and configures a Jakarta Mail Session with Gmail SMTP server settings.
     *
     * @return A configured {@link Session} object.
     */
    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });
    }

    /**
     * Sends an email notifying a subscriber of a late vehicle retrieval.
     *
     * @param toEmail  The subscriber's email address.
     * @param userName The subscriber's name.
     */
    public static void sendLateRetrievalEmail(String toEmail, String userName) {
        System.out.println("ENTERED sendLateRetrievalEmail");
        Session session = createSession();
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("BPark - Late Vehicle Retrieval");
            message.setText("Dear " + userName + ",\n\nThis is a notification that your vehicle was retrieved after the designated parking time.\n\n"
                    + "Please be aware that multiple late retrievals can lead to your account being temporarily frozen.\n\n"
                    + "Thank you for using BPark.\n\nThe BPark Team");
            Transport.send(message);
            System.out.println("Late retrieval email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send late retrieval email to " + toEmail);
            e.printStackTrace();
        }
    }

    /**
     * Sends an email notifying a user that their account has been frozen due to multiple late incidents.
     *
     * @param toEmail   The recipient's email address.
     * @param userName  The user's name.
     * @param lateCount The number of late incidents that triggered the freeze.
     */
    public static void sendAccountFrozenEmail(String toEmail, String userName, int lateCount) {
        System.out.println("ENTERED sendAccountFrozenEmail");
        Session session = createSession();
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("BPark - Important: Your Account Has Been Frozen");
            message.setText("Dear " + userName + ",\n\nYour BPark account has been automatically frozen due to accumulating " + lateCount + " late vehicle retrievals.\n\n"
                    + "To continue using our services, please contact customer support to reactivate your account.\n\n"
                    + "The BPark Team");
            Transport.send(message);
            System.out.println("Account frozen notification sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send account frozen email to " + toEmail);
            e.printStackTrace();
        }
    }
}