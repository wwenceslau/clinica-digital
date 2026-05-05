package com.clinicadigital.iam.application;

/**
 * Thrown when a bootstrap attempt is made and a super-user (profile 0)
 * already exists in the system.
 *
 * Mapped to OperationOutcome "conflict" at the CLI/API boundary.
 * Refs: FR-001, FR-009
 */
public class SuperUserAlreadyExistsException extends RuntimeException {

    public SuperUserAlreadyExistsException() {
        super("Super-user already exists");
    }
}
