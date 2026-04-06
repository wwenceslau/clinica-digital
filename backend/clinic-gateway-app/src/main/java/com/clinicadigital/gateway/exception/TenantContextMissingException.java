package com.clinicadigital.gateway.exception;

public class TenantContextMissingException extends RuntimeException {

    public TenantContextMissingException(String message) {
        super(message);
    }
}
