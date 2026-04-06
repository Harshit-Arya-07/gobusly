package com.busbooking.service;

import com.busbooking.dto.PaymentFailureRequestDto;
import com.busbooking.dto.PaymentHistoryResponseDto;
import com.busbooking.dto.PaymentOrderRequestDto;
import com.busbooking.dto.PaymentOrderResponseDto;
import com.busbooking.dto.PaymentVerifyRequestDto;
import com.busbooking.entity.Bus;
import com.busbooking.entity.PaymentHistory;
import com.busbooking.entity.PaymentStatus;
import com.busbooking.entity.Role;
import com.busbooking.entity.Seat;
import com.busbooking.entity.User;
import com.busbooking.exception.InvalidBookingException;
import com.busbooking.exception.ResourceNotFoundException;
import com.busbooking.exception.UnauthorizedAccessException;
import com.busbooking.repository.BusRepository;
import com.busbooking.repository.PaymentHistoryRepository;
import com.busbooking.repository.SeatRepository;
import com.busbooking.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final UserRepository userRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final EmailNotificationService emailNotificationService;

    @Value("${app.payment.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.payment.razorpay.key-secret}")
    private String razorpayKeySecret;


    @Override
    public PaymentOrderResponseDto createOrder(PaymentOrderRequestDto request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole().equals(Role.ADMIN)) {
            throw new UnauthorizedAccessException("Admin users are not allowed to create booking payments");
        }
        if (!currentUser.getId().equals(request.getUserId())) {
            throw new UnauthorizedAccessException("You can only pay for your own booking");
        }

        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        List<Integer> requestedSeatNumbers = request.getSeatNumbers().stream().distinct().toList();
        if (requestedSeatNumbers.isEmpty()) {
            throw new InvalidBookingException("At least one seat must be selected");
        }

        List<Seat> seats = seatRepository.findByBusIdAndSeatNumberIn(bus.getId(), requestedSeatNumbers);
        if (seats.size() != requestedSeatNumbers.size()) {
            throw new InvalidBookingException("One or more selected seats do not exist for this bus");
        }

        boolean hasBookedSeat = seats.stream().anyMatch(Seat::getIsBooked);
        if (hasBookedSeat) {
            throw new InvalidBookingException("One or more selected seats are already booked");
        }

        if (bus.getFareInRupees() == null || bus.getFareInRupees() <= 0) {
            throw new InvalidBookingException("Bus fare is not configured. Please contact admin.");
        }

        long amount = requestedSeatNumbers.size() * bus.getFareInRupees() * 100L;
        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String receipt = "BK" + shortId;

        PaymentHistory paymentHistory = PaymentHistory.builder()
                .userId(request.getUserId())
                .busId(bus.getId())
                .seatNumbers(new ArrayList<>(requestedSeatNumbers))
                .amountInRupees((int) (amount / 100L))
                .receipt(receipt)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);

            Order order = razorpayClient.orders.create(orderRequest);
            JSONObject createdOrder = new JSONObject(order.toString());
            String orderId = createdOrder.optString("id");

            paymentHistory.setOrderId(orderId);
            paymentHistory.setStatus(PaymentStatus.PENDING);
            paymentHistory.setFailureReason(null);
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);

            return PaymentOrderResponseDto.builder()
                    .keyId(razorpayKeyId)
                    .orderId(orderId)
                    .amount(amount)
                    .currency("INR")
                    .receipt(receipt)
                    .build();
        } catch (RazorpayException ex) {
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistory.setFailureReason("Unable to initialize payment: " + ex.getMessage());
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);
            throw new InvalidBookingException("Unable to initialize payment: " + ex.getMessage());
        } catch (Exception ex) {
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistory.setFailureReason("Unable to initialize payment");
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);
            throw new InvalidBookingException("Unable to initialize payment");
        }
    }

    @Override
    public void verifyPaymentSignature(PaymentVerifyRequestDto request) {
        PaymentHistory paymentHistory = paymentHistoryRepository.findByOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        validatePaymentAccess(paymentHistory.getUserId());

        try {
            String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
            String expectedSignature = generateHmacSHA256(payload, razorpayKeySecret);

            boolean isValid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8)
            );

            if (!isValid) {
                throw new InvalidBookingException("Payment signature verification failed");
            }

            paymentHistory.setStatus(PaymentStatus.SUCCESS);
            paymentHistory.setPaymentId(request.getRazorpayPaymentId());
            paymentHistory.setFailureReason(null);
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);

            User bookingUser = userRepository.findById(paymentHistory.getUserId())
                    .orElse(null);
            Bus bookingBus = busRepository.findById(paymentHistory.getBusId())
                    .orElse(null);
            if (bookingUser != null) {
                emailNotificationService.sendPaymentSuccessEmail(bookingUser, bookingBus, paymentHistory);
            }
        } catch (Exception ex) {
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistory.setPaymentId(request.getRazorpayPaymentId());
            paymentHistory.setFailureReason(ex.getMessage());
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);
            throw new InvalidBookingException("Payment verification failed: " + ex.getMessage());
        }
    }

    @Override
    public void markPaymentFailed(PaymentFailureRequestDto request) {
        PaymentHistory paymentHistory = paymentHistoryRepository.findByOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        validatePaymentAccess(paymentHistory.getUserId());

        paymentHistory.setStatus(PaymentStatus.FAILED);
        if (request.getRazorpayPaymentId() != null && !request.getRazorpayPaymentId().isBlank()) {
            paymentHistory.setPaymentId(request.getRazorpayPaymentId());
        }
        paymentHistory.setFailureReason(request.getReason() == null ? "Payment failed" : request.getReason());
        paymentHistory.setUpdatedAt(LocalDateTime.now());
        paymentHistoryRepository.save(paymentHistory);
    }

    @Override
    public List<PaymentHistoryResponseDto> getPaymentHistoryByUserId(String userId) {
        validatePaymentAccess(userId);

        return paymentHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapPaymentHistory)
                .toList();
    }

    @Override
    public List<PaymentHistoryResponseDto> getAllPaymentHistory() {
        User currentUser = getCurrentUser();
        if (!Role.ADMIN.equals(currentUser.getRole())) {
            throw new UnauthorizedAccessException("Admin access required");
        }

        return paymentHistoryRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapPaymentHistory)
                .toList();
    }

    private String generateHmacSHA256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private PaymentHistoryResponseDto mapPaymentHistory(PaymentHistory paymentHistory) {
        return PaymentHistoryResponseDto.builder()
                .id(paymentHistory.getId())
                .userId(paymentHistory.getUserId())
                .busId(paymentHistory.getBusId())
                .seatNumbers(paymentHistory.getSeatNumbers())
                .amountInRupees(paymentHistory.getAmountInRupees())
                .orderId(paymentHistory.getOrderId())
                .paymentId(paymentHistory.getPaymentId())
                .receipt(paymentHistory.getReceipt())
                .status(paymentHistory.getStatus().name())
                .failureReason(paymentHistory.getFailureReason())
                .createdAt(paymentHistory.getCreatedAt())
                .updatedAt(paymentHistory.getUpdatedAt())
                .build();
    }

    private void validatePaymentAccess(String userId) {
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().equals(Role.ADMIN) && !currentUser.getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only access your own payment history");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedAccessException("Unauthenticated access");
        }

        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = authentication.getName();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedAccessException("Current user not found"));
    }
}
