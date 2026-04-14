package com.busbooking.service;

import com.busbooking.entity.Bus;
import com.busbooking.entity.PaymentHistory;
import com.busbooking.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends email notifications for payment events.
 * <p>
 * The {@link #sendPaymentSuccessEmail} method is marked {@code @Async} so
 * that SMTP communication does not block the HTTP response to the client.
 * Requires {@code @EnableAsync} on the application class.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Async
    public void sendPaymentSuccessEmail(User user, Bus bus, PaymentHistory payment) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Skipping payment email: user {} has no email address", user.getId());
            return;
        }

        String busName = bus == null || bus.getBusName() == null || bus.getBusName().isBlank()
            ? "-"
            : bus.getBusName().trim();
        String busNumber = bus == null || bus.getBusNumber() == null || bus.getBusNumber().isBlank()
            ? "-"
            : bus.getBusNumber().trim();
        String busLabel = busName.equals(busNumber) ? busNumber : busName + " (" + busNumber + ")";
        String route = bus == null ? "-" : bus.getSource() + " to " + bus.getDestination();
        String departure = bus == null || bus.getTime() == null ? "-" : String.valueOf(bus.getTime());

        String subject = "Payment Successful - gobusly Booking";
        String body = "Hi " + user.getName() + ",\n\n"
                + "Your payment was successful.\n\n"
                + "Payment Details:\n"
                + "- Amount: Rs. " + payment.getAmountInRupees() + "\n"
                + "- Order ID: " + safe(payment.getOrderId()) + "\n"
                + "- Payment ID: " + safe(payment.getPaymentId()) + "\n"
                + "- Receipt: " + safe(payment.getReceipt()) + "\n"
                + "- Status: " + payment.getStatus().name() + "\n"
                + "- Time: " + payment.getUpdatedAt() + "\n\n"
                + "Bus Details:\n"
                + "- Bus: " + busLabel + "\n"
                + "- Route: " + route + "\n"
                + "- Departure: " + departure + "\n"
                + "- Seats: " + payment.getSeatNumbers() + "\n\n"
                + "Thank you for booking with gobusly.";

        try {
            String senderEmail = resolveSenderEmail();
            SimpleMailMessage message = new SimpleMailMessage();
            if (senderEmail != null && !senderEmail.isBlank()) {
                message.setFrom(senderEmail);
            }
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Payment success email sent: paymentId={}, to={}", payment.getId(), user.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send payment success email for payment {} to {}: {}",
                    payment.getId(), user.getEmail(), ex.getMessage(), ex);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String resolveSenderEmail() {
        if (fromEmail != null && !fromEmail.isBlank()) {
            return fromEmail.trim();
        }

        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername.trim();
        }

        return null;
    }
}
