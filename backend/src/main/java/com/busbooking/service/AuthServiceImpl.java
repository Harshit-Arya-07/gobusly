package com.busbooking.service;

import com.busbooking.dto.AuthRequestDto;
import com.busbooking.dto.AuthResponseDto;
import com.busbooking.dto.ApiMessageResponseDto;
import com.busbooking.dto.ResendEmailOtpRequestDto;
import com.busbooking.dto.RegisterRequestDto;
import com.busbooking.dto.VerifyEmailOtpRequestDto;
import com.busbooking.entity.Role;
import com.busbooking.entity.User;
import com.busbooking.exception.DuplicateEmailException;
import com.busbooking.exception.EmailNotVerifiedException;
import com.busbooking.exception.InvalidBookingException;
import com.busbooking.repository.UserRepository;
import com.busbooking.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
        private final EmailNotificationService emailNotificationService;

        private static final int OTP_LENGTH = 6;
        private static final long OTP_EXPIRY_MINUTES = 10;

        @Value("${app.auth.admin-signup-code:}")
        private String adminSignupCode;

    @Override
        public ApiMessageResponseDto register(RegisterRequestDto request) {
                String email = normalizeEmail(request.getEmail());
                if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email is already in use");
        }

        Role requestedRole = parseRequestedRole(request.getRole());
        if (Role.ADMIN.equals(requestedRole)) {
            if (adminSignupCode == null || adminSignupCode.isBlank()) {
                throw new InvalidBookingException("Admin signup is disabled. Please contact system administrator.");
            }

            if (request.getAdminSignupCode() == null || request.getAdminSignupCode().isBlank()
                    || !adminSignupCode.equals(request.getAdminSignupCode())) {
                throw new InvalidBookingException("Invalid admin signup code");
            }
        }

        User user = User.builder()
                .name(request.getName())
                                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                                .emailVerified(Boolean.FALSE)
                .build();

                saveUserWithFreshOtp(user);
                log.info("New user registered and OTP generated: email={}", user.getEmail());

                emailNotificationService.sendEmailVerificationOtp(user.getEmail(), user.getName(), user.getEmailVerificationOtp());

                return new ApiMessageResponseDto("Registration successful. Please verify your email using the OTP sent to your inbox.");
        }

        @Override
        public AuthResponseDto verifyEmailOtp(VerifyEmailOtpRequestDto request) {
                String email = normalizeEmail(request.getEmail());
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new InvalidBookingException("Invalid or expired OTP"));

                if (isEmailVerified(user)) {
                        String token = generateTokenForUser(user);
                        return buildAuthResponse(user, token);
                }

                if (user.getEmailVerificationOtp() == null
                                || user.getEmailVerificationOtpExpiresAt() == null
                                || user.getEmailVerificationOtpExpiresAt().isBefore(LocalDateTime.now())
                                || !user.getEmailVerificationOtp().equals(request.getOtp())) {
                        throw new InvalidBookingException("Invalid or expired OTP");
                }

                user.setEmailVerified(Boolean.TRUE);
                user.setEmailVerifiedAt(LocalDateTime.now());
                user.setEmailVerificationOtp(null);
                user.setEmailVerificationOtpExpiresAt(null);
                User verifiedUser = userRepository.save(user);

                log.info("Email verified successfully: id={}, email={}", verifiedUser.getId(), verifiedUser.getEmail());

                String token = generateTokenForUser(verifiedUser);
                return buildAuthResponse(verifiedUser, token);
        }

        @Override
        public ApiMessageResponseDto resendEmailOtp(ResendEmailOtpRequestDto request) {
                String email = normalizeEmail(request.getEmail());
                Optional<User> optionalUser = userRepository.findByEmail(email);

                if (optionalUser.isEmpty()) {
                        return new ApiMessageResponseDto("If the email exists, a verification OTP has been sent.");
                }

                User user = optionalUser.get();
                if (isEmailVerified(user)) {
                        return new ApiMessageResponseDto("Email is already verified. Please login.");
                }

                saveUserWithFreshOtp(user);
                emailNotificationService.sendEmailVerificationOtp(user.getEmail(), user.getName(), user.getEmailVerificationOtp());

                return new ApiMessageResponseDto("Verification OTP sent successfully.");
        }

        private void saveUserWithFreshOtp(User user) {
                user.setEmailVerificationOtp(generateOtp());
                user.setEmailVerificationOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
                userRepository.save(user);
        }

        private String generateOtp() {
                int min = (int) Math.pow(10, OTP_LENGTH - 1);
                int max = (int) Math.pow(10, OTP_LENGTH) - 1;
                return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
        }

        private boolean isEmailVerified(User user) {
                // Legacy users may not have this field in older documents.
                return user.getEmailVerified() == null || Boolean.TRUE.equals(user.getEmailVerified());
        }

        private String normalizeEmail(String email) {
                if (email == null) {
                        return null;
                }
                return email.trim().toLowerCase(Locale.ROOT);
        }

        private AuthResponseDto buildAuthResponse(User user, String token) {
                return AuthResponseDto.builder()
                                .token(token)
                                .userId(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(user.getRole().name())
                                .build();
        }

        private Role parseRequestedRole(String roleValue) {
                if (roleValue == null || roleValue.isBlank()) {
                        return Role.USER;
                }

                try {
                        return Role.valueOf(roleValue.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                        throw new InvalidBookingException("Invalid role. Allowed values are USER and ADMIN.");
                }
        }

    @Override
    public AuthResponseDto login(AuthRequestDto request) {
                String email = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

                User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidBookingException("Invalid email or password"));

                if (!isEmailVerified(user)) {
                        saveUserWithFreshOtp(user);
                        emailNotificationService.sendEmailVerificationOtp(user.getEmail(), user.getName(), user.getEmailVerificationOtp());
                        throw new EmailNotVerifiedException("Email not verified. A new OTP has been sent to your email.");
                }

        log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());

        String token = generateTokenForUser(user);

                return buildAuthResponse(user, token);
    }

    /**
     * Builds a Spring Security {@link UserDetails} from the domain user
     * and generates a JWT token.
     */
    private String generateTokenForUser(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        return jwtService.generateToken(userDetails);
    }
}
