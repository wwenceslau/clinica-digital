package com.clinicadigital.iam.application;

/**
 * T095 Thrown when an attempt to create a profile-20 user fails because the
 * given email is already registered within the same tenant.
 *
 * Maps to HTTP 409 Conflict and OperationOutcome code=conflict.
 * Refs: FR-009
 */
public class EmailAlreadyTakenException extends RuntimeException {

    private final String email;

    public EmailAlreadyTakenException(String email) {
        super("Email already exists in tenant: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
