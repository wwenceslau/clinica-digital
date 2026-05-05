package com.clinicadigital.iam.application;

/**
 * Thrown when an attempt to create a tenant admin fails because the given
 * email is already registered as an admin (profile=10) in any tenant.
 *
 * Maps to HTTP 409 Conflict and CLI OperationOutcome code=conflict.
 * Refs: FR-009
 */
public class AdminEmailAlreadyExistsException extends RuntimeException {

    private final String email;

    public AdminEmailAlreadyExistsException(String email) {
        super("Admin email already registered: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
