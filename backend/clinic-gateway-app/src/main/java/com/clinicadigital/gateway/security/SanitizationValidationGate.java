package com.clinicadigital.gateway.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SanitizationValidationGate {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    public String sanitizeFreeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return CONTROL_CHARS.matcher(trimmed).replaceAll("");
    }

    public String requireEmail(String value, String fieldName) {
        String sanitized = sanitizeFreeText(value);
        if (sanitized == null || sanitized.isBlank() || !sanitized.contains("@")) {
            throw new IllegalArgumentException(fieldName + " must be a valid email");
        }
        return sanitized;
    }

    public String requirePassword(String value, String fieldName) {
        String sanitized = sanitizeFreeText(value);
        if (sanitized == null || sanitized.length() < 8) {
            throw new IllegalArgumentException(fieldName + " must have at least 8 chars");
        }
        return sanitized;
    }
}
