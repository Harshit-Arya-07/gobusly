package com.busbooking.service;

import com.busbooking.dto.PageResponseDto;
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
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final EmailNotificationService emailNotificationService;
    private final SeatLockService seatLockService;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;

    @Value("${app.payment.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${app.payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Override
    public PaymentOrderResponseDto createOrder(PaymentOrderRequestDto request) {
        validateRazorpayConfiguration();
        User currentUser = authenticatedUserService.getCurrentUser();

        if (currentUser.getRole().equals(Role.ADMIN)) {
            throw new UnauthorizedAccessException("Admin users are not allowed to create booking payments");
        }
        if (!currentUser.getId().equals(request.getUserId())) {
            throw new UnauthorizedAccessException("You can only pay for your own booking");
        }

        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + request.getBusId()));

        List<Integer> requestedSeatNumbers = request.getSeatNumbers().stream().distinct().sorted().toList();
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

        // Idempotency: if a PENDING order already exists for the same user/bus/seats, return it
        Optional<PaymentHistory> existingPending = paymentHistoryRepository
                .findByUserIdAndBusIdAndSeatNumbersAndStatus(
                        request.getUserId(), bus.getId(), requestedSeatNumbers, PaymentStatus.PENDING);
        if (existingPending.isPresent()) {
            PaymentHistory existing = existingPending.get();
            log.info("Returning existing pending order: orderId={}, userId={}", existing.getOrderId(), currentUser.getId());
            return PaymentOrderResponseDto.builder()
                    .keyId(razorpayKeyId)
                    .orderId(existing.getOrderId())
                    .amount(existing.getAmountInRupees() * 100L)
                    .currency("INR")
                    .receipt(existing.getReceipt())
                    .build();
        }

        seatLockService.lockSeats(bus.getId(), requestedSeatNumbers, currentUser.getId());

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

            log.info("Payment order created: orderId={}, userId={}, busId={}, amount={}",
                    orderId, currentUser.getId(), bus.getId(), amount);

            return PaymentOrderResponseDto.builder()
                    .keyId(razorpayKeyId)
                    .orderId(orderId)
                    .amount(amount)
                    .currency("INR")
                    .receipt(receipt)
                    .build();
        } catch (RazorpayException ex) {
            seatLockService.releaseSeatLocks(bus.getId(), requestedSeatNumbers, currentUser.getId());
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistory.setFailureReason("Unable to initialize payment: " + ex.getMessage());
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);
            log.error("Razorpay order creation failed for userId={}, busId={}: {}",
                    currentUser.getId(), bus.getId(), ex.getMessage());
            throw new InvalidBookingException("Unable to initialize payment: " + ex.getMessage());
        } catch (Exception ex) {
            seatLockService.releaseSeatLocks(bus.getId(), requestedSeatNumbers, currentUser.getId());
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistory.setFailureReason("Unable to initialize payment");
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);
            log.error("Payment order creation failed unexpectedly for userId={}, busId={}",
                    currentUser.getId(), bus.getId(), ex);
            throw new InvalidBookingException("Unable to initialize payment");
        }
    }

    @Override
    public void verifyPaymentSignature(PaymentVerifyRequestDto request) {
        validateRazorpayConfiguration();
        PaymentHistory paymentHistory = findLatestPaymentByOrderId(request.getRazorpayOrderId());
        validatePaymentAccess(paymentHistory.getUserId());

        // Idempotency guard: if already verified, skip re-verification
        if (PaymentStatus.SUCCESS.equals(paymentHistory.getStatus())) {
            log.info("Payment already verified (idempotent): orderId={}", request.getRazorpayOrderId());
            return;
        }

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

            log.info("Payment verified: orderId={}, paymentId={}, userId={}",
                    request.getRazorpayOrderId(), request.getRazorpayPaymentId(), paymentHistory.getUserId());

                User bookingUser = userRepository.findById(paymentHistory.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + paymentHistory.getUserId()));
            Bus bookingBus = busRepository.findById(paymentHistory.getBusId()).orElse(null);
            emailNotificationService.sendPaymentSuccessEmail(bookingUser, bookingBus, paymentHistory);

        } catch (InvalidBookingException ex) {
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistory.setPaymentId(request.getRazorpayPaymentId());
            paymentHistory.setFailureReason(ex.getMessage());
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);
            throw ex;
        } catch (Exception ex) {
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistory.setPaymentId(request.getRazorpayPaymentId());
            paymentHistory.setFailureReason(ex.getMessage());
            paymentHistory.setUpdatedAt(LocalDateTime.now());
            paymentHistoryRepository.save(paymentHistory);
            log.error("Payment verification failed for orderId={}: {}",
                    request.getRazorpayOrderId(), ex.getMessage());
            throw new InvalidBookingException("Payment verification failed: " + ex.getMessage());
        }
    }

    @Override
    public void markPaymentFailed(PaymentFailureRequestDto request) {
        PaymentHistory paymentHistory = findLatestPaymentByOrderId(request.getRazorpayOrderId());
        validatePaymentAccess(paymentHistory.getUserId());

        seatLockService.releaseSeatLocks(paymentHistory.getBusId(), paymentHistory.getSeatNumbers(), paymentHistory.getUserId());

        paymentHistory.setStatus(PaymentStatus.FAILED);
        if (request.getRazorpayPaymentId() != null && !request.getRazorpayPaymentId().isBlank()) {
            paymentHistory.setPaymentId(request.getRazorpayPaymentId());
        }
        paymentHistory.setFailureReason(request.getReason() == null ? "Payment failed" : request.getReason());
        paymentHistory.setUpdatedAt(LocalDateTime.now());
        paymentHistoryRepository.save(paymentHistory);

        log.info("Payment marked as failed: orderId={}, reason={}", request.getRazorpayOrderId(), request.getReason());
    }

    @Override
    public PageResponseDto<PaymentHistoryResponseDto> getPaymentHistoryByUserId(String userId, int page, int size) {
        validatePaymentAccess(userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentHistory> paymentPage = paymentHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return buildPageResponse(paymentPage);
    }

    @Override
    public PageResponseDto<PaymentHistoryResponseDto> getAllPaymentHistory(int page, int size) {
        throw new UnauthorizedAccessException("Admin role can only add buses");
    }

    private String generateHmacSHA256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private PaymentHistoryResponseDto mapPaymentHistory(PaymentHistory paymentHistory) {
        Bus bus = paymentHistory.getBusId() == null ? null : busRepository.findById(paymentHistory.getBusId()).orElse(null);
        return PaymentHistoryResponseDto.builder()
                .id(paymentHistory.getId())
                .userId(paymentHistory.getUserId())
                .busId(paymentHistory.getBusId())
            .busName(bus == null ? null : bus.getBusName())
                .seatNumbers(paymentHistory.getSeatNumbers())
                .amountInRupees(paymentHistory.getAmountInRupees())
                .orderId(paymentHistory.getOrderId())
                .paymentId(paymentHistory.getPaymentId())
                .receipt(paymentHistory.getReceipt())
                .status(paymentHistory.getStatus() == null ? "PENDING" : paymentHistory.getStatus().name())
                .failureReason(paymentHistory.getFailureReason())
                .createdAt(paymentHistory.getCreatedAt())
                .updatedAt(paymentHistory.getUpdatedAt())
                .build();
    }

    private PageResponseDto<PaymentHistoryResponseDto> buildPageResponse(Page<PaymentHistory> paymentPage) {
        List<PaymentHistoryResponseDto> content = paymentPage.getContent()
                .stream()
                .map(this::mapPaymentHistory)
                .toList();

        return PageResponseDto.<PaymentHistoryResponseDto>builder()
                .content(content)
                .page(paymentPage.getNumber())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .last(paymentPage.isLast())
                .build();
    }

    private void validatePaymentAccess(String userId) {
        User currentUser = authenticatedUserService.getCurrentUser();
        if (!currentUser.getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only access your own payment history");
        }
    }

    private PaymentHistory findLatestPaymentByOrderId(String orderId) {
        List<PaymentHistory> matches = paymentHistoryRepository.findByOrderIdOrderByUpdatedAtDesc(orderId);
        if (matches == null || matches.isEmpty()) {
            throw new ResourceNotFoundException("Payment order not found");
        }

        if (matches.size() > 1) {
            log.warn("Multiple payment history records found for same orderId={}, using latest", orderId);
        }

        return matches.get(0);
    }

    private void validateRazorpayConfiguration() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new InvalidBookingException("Razorpay keys are missing. Set APP_PAYMENT_RAZORPAY_KEY_ID and APP_PAYMENT_RAZORPAY_KEY_SECRET.");
        }
    }
}
