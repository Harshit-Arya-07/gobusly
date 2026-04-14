package com.busbooking.service;

import com.busbooking.dto.AuthRequestDto;
import com.busbooking.dto.AuthResponseDto;
import com.busbooking.dto.RegisterRequestDto;
import com.busbooking.entity.Role;
import com.busbooking.entity.User;
import com.busbooking.exception.DuplicateEmailException;
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

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

        @Value("${app.auth.admin-signup-code:}")
        private String adminSignupCode;

    @Override
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
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
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        String token = generateTokenForUser(savedUser);

        return AuthResponseDto.builder()
                .token(token)
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidBookingException("Invalid email or password"));

        log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());

        String token = generateTokenForUser(user);

        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
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
