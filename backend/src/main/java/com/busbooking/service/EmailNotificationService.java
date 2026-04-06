package com.busbooking.service;

import com.busbooking.entity.Bus;
import com.busbooking.entity.PaymentHistory;
import com.busbooking.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendPaymentSuccessEmail(User user, Bus bus, PaymentHistory payment) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        String busNumber = bus == null ? "-" : bus.getBusNumber();
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
                + "- Bus: " + busNumber + "\n"
                + "- Route: " + route + "\n"
                + "- Departure: " + departure + "\n"
                + "- Seats: " + payment.getSeatNumbers() + "\n\n"
                + "Thank you for booking with gobusly.";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send payment success email for payment {}: {}", payment.getId(), ex.getMessage());
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
