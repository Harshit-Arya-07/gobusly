package com.busbooking.service;

import com.busbooking.entity.User;
import com.busbooking.exception.UnauthorizedAccessException;
import com.busbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Centralized service for retrieving the currently authenticated user
 * from the Spring Security context. Replaces the duplicated
 * {@code getCurrentUser()} methods that were scattered across
 * BookingServiceImpl, BusServiceImpl, and PaymentServiceImpl.
 */
@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    /**
     * Returns the {@link User} entity for the currently authenticated request.
     *
     * @throws UnauthorizedAccessException if there is no authentication
     *         or the user cannot be found in the database.
     */
    public User getCurrentUser() {
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
