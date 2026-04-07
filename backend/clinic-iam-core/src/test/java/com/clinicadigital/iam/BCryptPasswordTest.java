package com.clinicadigital.iam;

import com.clinicadigital.iam.application.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T085 [US3] — Unit test for PasswordService (BCryptPasswordEncoder hash + verify).
 *
 * <p><b>TDD state</b>: RED until {@link PasswordService} (T098, Phase 5.B) is
 * implemented with BCrypt cost 12.
 *
 * <p>Contract verified:
 * <ul>
 *   <li>hashPassword returns non-null, non-blank BCrypt string.</li>
 *   <li>BCrypt format — hash starts with {@code $2} prefix.</li>
 *   <li>Different invocations for the same plaintext produce different hashes (salt uniqueness).</li>
 *   <li>verifyPassword returns {@code true} for correct plaintext + hash.</li>
 *   <li>verifyPassword returns {@code false} for wrong plaintext.</li>
 *   <li>verifyPassword returns {@code false} for null or blank plaintext (fail-closed).</li>
 * </ul>
 *
 * Refs: FR-006, research.md (BCrypt cost 12, fail-closed)
 */
class BCryptPasswordTest {

    private PasswordService service;

    @BeforeEach
    void setUp() {
        service = new PasswordService();
    }

    @Test
    void hashPasswordMustReturnNonNullNonBlankString() {
        String hash = service.hashPassword("secret123");
        assertNotNull(hash, "hash must not be null");
        assertFalse(hash.isBlank(), "hash must not be blank");
    }

    @Test
    void hashPasswordMustUseBCryptFormat() {
        String hash = service.hashPassword("secret123");
        // BCrypt hashes always start with $2a$, $2b$, or $2y$
        assertTrue(hash.startsWith("$2"), "hash must use BCrypt format (research.md: BCrypt cost 12)");
    }

    @Test
    void samePasswordProducesDifferentHashesDueToSalt() {
        String hash1 = service.hashPassword("password");
        String hash2 = service.hashPassword("password");
        assertNotEquals(hash1, hash2,
                "BCrypt salt must produce unique ciphertext on each call (FR-006)");
    }

    @Test
    void verifyPasswordReturnsTrueForCorrectPassword() {
        String plaintext = "correct-horse-battery-staple";
        String hash = service.hashPassword(plaintext);
        assertTrue(service.verifyPassword(plaintext, hash),
                "verifyPassword must return true when plaintext matches hash (FR-006)");
    }

    @Test
    void verifyPasswordReturnsFalseForWrongPassword() {
        String hash = service.hashPassword("correct");
        assertFalse(service.verifyPassword("wrong", hash),
                "verifyPassword must return false for wrong plaintext (fail-closed, FR-006)");
    }

    @Test
    void verifyPasswordReturnsFalseForNullRaw() {
        String hash = service.hashPassword("secret");
        assertFalse(service.verifyPassword(null, hash),
                "verifyPassword must return false for null input (fail-closed)");
    }

    @Test
    void verifyPasswordReturnsFalseForBlankRaw() {
        String hash = service.hashPassword("secret");
        assertFalse(service.verifyPassword("", hash),
                "verifyPassword must return false for blank input (fail-closed)");
    }

    @Test
    void hashPasswordRejectsNullInput() {
        assertThrows(IllegalArgumentException.class,
                () -> service.hashPassword(null),
                "hashPassword must reject null input");
    }

    @Test
    void hashPasswordRejectsBlankInput() {
        assertThrows(IllegalArgumentException.class,
                () -> service.hashPassword("   "),
                "hashPassword must reject blank input");
    }
}
