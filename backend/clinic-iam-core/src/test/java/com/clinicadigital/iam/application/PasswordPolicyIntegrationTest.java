package com.clinicadigital.iam.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T066 [P] [US9] — Integration test for {@link PasswordService} robust hashing policy.
 *
 * <p>Verifies the BCrypt (cost 12) password hashing contract (FR-009):
 * <ul>
 *   <li>Hashed passwords use BCrypt format ($2…).</li>
 *   <li>BCrypt cost factor 12 is embedded in the hash string.</li>
 *   <li>Salt is unique per hash (same input → different hash).</li>
 *   <li>Correct password is verified; wrong password is rejected.</li>
 *   <li>Null/too-short/too-long passwords are rejected before hashing.</li>
 *   <li>{@code verifyPassword(null, hash)} is fail-closed (returns false).</li>
 * </ul>
 *
 * <p>No database required — pure Java, no Spring context.
 *
 * Refs: FR-009, Constitution Art. VI, T069
 */
class PasswordPolicyIntegrationTest {

    private PasswordService service;

    @BeforeEach
    void setUp() {
        service = new PasswordService();
    }

    // ── BCrypt format and cost factor ─────────────────────────────────────

    @Test
    void hash_usesBCryptFormat() {
        String hash = service.hashPassword("ValidPass1");
        assertTrue(hash.startsWith("$2"),
                "BCrypt hash must start with $2 (BCrypt version prefix)");
    }

    @Test
    void hash_usesCostFactor12() {
        String hash = service.hashPassword("ValidPass1");
        assertTrue(hash.contains("$12$"),
                "BCrypt hash must embed cost factor 12 ($12$)");
    }

    // ── Salt uniqueness ───────────────────────────────────────────────────

    @Test
    void samePasswordProducesDifferentHashes_dueToBCryptSalt() {
        String hash1 = service.hashPassword("SamePass99");
        String hash2 = service.hashPassword("SamePass99");

        assertNotEquals(hash1, hash2,
                "BCrypt must produce unique hashes for the same password (different salt each time)");
    }

    // ── Verification ──────────────────────────────────────────────────────

    @Test
    void correctPasswordVerifiesSuccessfully() {
        String raw = "CorrectPassword!";
        String hash = service.hashPassword(raw);

        assertTrue(service.verifyPassword(raw, hash),
                "correct password must pass verification");
    }

    @Test
    void wrongPasswordFailsVerification() {
        String hash = service.hashPassword("OriginalPassword99");

        assertFalse(service.verifyPassword("WrongPassword99", hash),
                "wrong password must fail verification");
    }

    @Test
    void verifyWithNullRaw_returnsFalseFailClosed() {
        String hash = service.hashPassword("SomeValidPassword");

        assertFalse(service.verifyPassword(null, hash),
                "null raw password must return false (fail-closed policy)");
    }

    @Test
    void verifyWithNullHash_returnsFalseFailClosed() {
        assertFalse(service.verifyPassword("SomePassword1", null),
                "null hash must return false (fail-closed policy)");
    }

    // ── Policy enforcement — minimum length ───────────────────────────────

    @Test
    void hashPassword_belowMinLength_throwsIllegalArgument() {
        // 7 characters — exactly one below the minimum of 8
        assertThrows(IllegalArgumentException.class,
                () -> service.hashPassword("Short1!"),
                "password below minimum length must be rejected");
    }

    @Test
    void hashPassword_exactlyMinLength_succeeds() {
        // 8 characters — exactly at the minimum
        assertDoesNotThrow(() -> service.hashPassword("Valid8!x"),
                "password at minimum length (8 chars) must be accepted");
    }

    // ── Policy enforcement — maximum length ───────────────────────────────

    @Test
    void hashPassword_aboveMaxLength_throwsIllegalArgument() {
        // 101 characters — one above the maximum of 100
        String tooLong = "A".repeat(100) + "!";
        assertThrows(IllegalArgumentException.class,
                () -> service.hashPassword(tooLong),
                "password above maximum length must be rejected");
    }

    @Test
    void hashPassword_exactlyMaxLength_succeeds() {
        // 100 characters — exactly at the maximum
        String atMax = "P".repeat(99) + "1";
        assertDoesNotThrow(() -> service.hashPassword(atMax),
                "password at maximum length (100 chars) must be accepted");
    }

    // ── Policy enforcement — null ─────────────────────────────────────────

    @Test
    void hashPassword_null_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.hashPassword(null),
                "null password must be rejected");
    }

    @Test
    void hashPassword_blank_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.hashPassword("        "),
                "blank password must be rejected");
    }

    // ── Constants are publicly available ─────────────────────────────────

    @Test
    void passwordConstants_areExposed() {
        assertEquals(12, PasswordService.BCRYPT_COST,
                "BCRYPT_COST must be 12 (FR-009)");
        assertEquals(8, PasswordService.PASSWORD_MIN_LENGTH,
                "PASSWORD_MIN_LENGTH must be 8");
        assertEquals(100, PasswordService.PASSWORD_MAX_LENGTH,
                "PASSWORD_MAX_LENGTH must be 100 (protection against BCrypt DoS)");
    }
}
