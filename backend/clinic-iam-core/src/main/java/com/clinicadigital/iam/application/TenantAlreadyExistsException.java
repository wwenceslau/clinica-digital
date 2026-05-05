package com.clinicadigital.iam.application;

/**
 * Thrown when an attempt to create a new tenant fails because a conflicting
 * organization already exists (CNES or display_name collision).
 *
 * Maps to HTTP 409 Conflict and CLI OperationOutcome code=conflict.
 * Refs: FR-009
 */
public class TenantAlreadyExistsException extends RuntimeException {

    private final String field;
    private final String value;

    public TenantAlreadyExistsException(String field, String value) {
        super("Tenant already exists: " + field + "=" + value);
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
