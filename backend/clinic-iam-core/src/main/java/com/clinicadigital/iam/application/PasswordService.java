package com.clinicadigital.iam.application;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Stub for TDD compilation — Phase 5.A (T085).
 * Real BCrypt implementation is in T098 (Phase 5.B).
 *
 * Refs: FR-006, research.md (BCrypt cost 12)
 */
@Service
public class PasswordService {

    private static final int BCRYPT_COST = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_COST);

    /**
     * Hash a raw-text password using BCrypt with cost 12.
     *
     * @param raw plaintext password (must not be null or blank)
     * @return BCrypt-encoded hash string
     */
    public String hashPassword(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("raw password must not be null or blank");
        }
        return encoder.encode(raw);
    }

    /**
     * Verify that a plaintext password matches a BCrypt-encoded hash.
     *
     * @param raw     plaintext candidate
     * @param encoded BCrypt hash
     * @return true if match, false otherwise (including null/blank raw)
     */
    public boolean verifyPassword(String raw, String encoded) {
        if (raw == null || raw.isBlank() || encoded == null || encoded.isBlank()) {
            return false;
        }
        return encoder.matches(raw, encoded);
    }
}
