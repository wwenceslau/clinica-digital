package com.clinicadigital.iam.application;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * T069 [US9] — Password hashing policy using BCrypt cost 12.
 *
 * <p><b>Policy (FR-011, Constitution Art. VI)</b>:
 * <ul>
 *   <li>Algorithm: BCrypt with cost factor {@value #BCRYPT_COST} (Argon2id is an
 *       accepted alternative per spec, but BCrypt cost 12 satisfies "hash forte"
 *       without an additional native dependency).</li>
 *   <li>Minimum length: {@value #PASSWORD_MIN_LENGTH} characters.</li>
 *   <li>Maximum length: {@value #PASSWORD_MAX_LENGTH} characters
 *       (guards against DoS via excessively long inputs).</li>
 *   <li>The hash is one-way and non-reversible.</li>
 * </ul>
 *
 * Refs: FR-011
 */
@Service
public class PasswordService {

    /** BCrypt work factor. Must be >= 12 per security policy. */
    public static final int BCRYPT_COST = 12;

    /** Minimum accepted raw-password length. */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /**
     * Maximum accepted raw-password length.
     * BCrypt silently truncates at 72 bytes; enforcing an explicit upper bound
     * prevents DoS via huge inputs and avoids silent truncation surprises.
     */
    public static final int PASSWORD_MAX_LENGTH = 100;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_COST);

    /**
     * Hash a raw-text password using BCrypt with cost {@value #BCRYPT_COST}.
     *
     * @param raw plaintext password; must satisfy the length policy
     * @return BCrypt-encoded hash string
     * @throws IllegalArgumentException if {@code raw} is null, blank, shorter than
     *                                  {@value #PASSWORD_MIN_LENGTH}, or longer than
     *                                  {@value #PASSWORD_MAX_LENGTH} characters
     */
    public String hashPassword(String raw) {
        validatePolicy(raw);
        return encoder.encode(raw);
    }

    /**
     * Verify that a plaintext password matches a BCrypt-encoded hash.
     *
     * @param raw     plaintext candidate
     * @param encoded BCrypt hash
     * @return {@code true} if match; {@code false} for null/blank/wrong input
     *         (fail-closed — never throws for invalid inputs)
     */
    public boolean verifyPassword(String raw, String encoded) {
        if (raw == null || raw.isBlank() || encoded == null || encoded.isBlank()) {
            return false;
        }
        try {
            return encoder.matches(raw, encoded);
        } catch (Exception e) {
            return false;
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private static void validatePolicy(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("password must not be null or blank");
        }
        if (raw.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "password must be at least " + PASSWORD_MIN_LENGTH + " characters");
        }
        if (raw.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "password must not exceed " + PASSWORD_MAX_LENGTH + " characters");
        }
    }
}
