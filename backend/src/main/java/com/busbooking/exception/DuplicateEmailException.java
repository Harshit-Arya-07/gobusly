package com.busbooking.exception;

/**
 * Thrown when a user attempts to register with an email address
 * that is already associated with an existing account.
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
