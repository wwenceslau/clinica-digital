package com.clinicadigital.iam.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T065 [P] [US9] — Integration test for CPF/PII encryption via {@link PiiCryptoService}.
 *
 * <p>Verifies the AES-256-GCM encryption contract (FR-011):
 * <ul>
 *   <li>Encrypt + decrypt roundtrip preserves the original plaintext.</li>
 *   <li>Two encryptions of the same plaintext produce different ciphertexts (IV randomness).</li>
 *   <li>Null or blank plaintext is rejected before encryption.</li>
 *   <li>Ciphertext tampered with triggers authentication-tag failure.</li>
 *   <li>Wrong key version triggers decryption failure.</li>
 *   <li>Rotate re-encrypts under the new active key and remains decryptable.</li>
 *   <li>Stored format: at least {@code IV_LENGTH + 1} bytes (non-empty ciphertext).</li>
 * </ul>
 *
 * <p>No database required: AES-256-GCM is implemented in Java ({@code javax.crypto}).
 *
 * Refs: FR-011, Constitution Art. VI, T068
 */
class PiiEncryptionIntegrationTest {

    private static final String KEY_V1 = "secret-key-for-pii-encryption-v1";
    private static final String KEY_V2 = "secret-key-for-pii-encryption-v2";

    private PiiCryptoService service;

    @BeforeEach
    void setUp() {
        EncryptionKeyProvider provider = new EncryptionKeyProvider() {
            @Override
            public String activeKeyVersion() {
                return "v1";
            }

            @Override
            public String keyMaterialForVersion(String version) {
                return switch (version) {
                    case "v1" -> KEY_V1;
                    case "v2" -> KEY_V2;
                    default -> throw new IllegalArgumentException("unknown key version: " + version);
                };
            }
        };
        service = new PiiCryptoService(provider);
    }

    // ── Roundtrip ──────────────────────────────────────────────────────────

    @Test
    void encryptThenDecrypt_returnsSamePlaintext() {
        String cpf = "123.456.789-00";

        PiiCryptoService.EncryptedValue encrypted = service.encrypt(cpf);
        String decrypted = service.decrypt(encrypted.cipherText(), encrypted.keyVersion());

        assertEquals(cpf, decrypted, "decrypt(encrypt(plaintext)) must equal the original plaintext (FR-011)");
    }

    @Test
    void encryptedBytesAreNeverPlaintext() {
        String cpf = "123.456.789-00";
        PiiCryptoService.EncryptedValue encrypted = service.encrypt(cpf);

        // The stored bytes must not contain the CPF in UTF-8 form
        String rawStored = new String(encrypted.cipherText());
        assertFalse(rawStored.contains(cpf),
                "encrypted bytes must not contain the plaintext CPF");
    }

    // ── IV randomness (semantic security) ─────────────────────────────────

    @Test
    void sameInputProducesDifferentCiphertexts_dueToRandomIv() {
        String cpf = "987.654.321-00";

        PiiCryptoService.EncryptedValue first = service.encrypt(cpf);
        PiiCryptoService.EncryptedValue second = service.encrypt(cpf);

        assertNotEquals(
                java.util.Arrays.toString(first.cipherText()),
                java.util.Arrays.toString(second.cipherText()),
                "each encryption must use a fresh IV, producing distinct ciphertexts");
    }

    // ── Stored-format size check ───────────────────────────────────────────

    @Test
    void encryptedValueHasSufficientLength() {
        // Minimum: 12 bytes IV + 1 byte plaintext + 16 bytes GCM auth tag = 29 bytes
        PiiCryptoService.EncryptedValue encrypted = service.encrypt("x");

        assertTrue(encrypted.cipherText().length >= 29,
                "stored ciphertext must include IV (12 B) + data + GCM tag (16 B)");
    }

    // ── Key version tracking ───────────────────────────────────────────────

    @Test
    void encryptedValueCarriesActiveKeyVersion() {
        PiiCryptoService.EncryptedValue encrypted = service.encrypt("cpf-value");

        assertEquals("v1", encrypted.keyVersion(),
                "EncryptedValue must carry the active key version for rotation support");
    }

    // ── Input validation ───────────────────────────────────────────────────

    @Test
    void encryptBlankInput_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.encrypt(""),
                "blank plaintext must be rejected before encryption");
    }

    @Test
    void encryptNullInput_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.encrypt(null),
                "null plaintext must be rejected before encryption");
    }

    @Test
    void decryptEmptyCiphertext_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.decrypt(new byte[0], "v1"));
    }

    @Test
    void decryptNullVersion_throwsIllegalArgument() {
        PiiCryptoService.EncryptedValue encrypted = service.encrypt("cpf");
        assertThrows(IllegalArgumentException.class,
                () -> service.decrypt(encrypted.cipherText(), null));
    }

    // ── Tamper detection (GCM authentication tag) ─────────────────────────

    @Test
    void tamperingWithCiphertext_causesDecryptionFailure() {
        PiiCryptoService.EncryptedValue encrypted = service.encrypt("sensitive-cpf");
        byte[] tampered = encrypted.cipherText().clone();
        // Flip a bit in the ciphertext portion (after the 12-byte IV)
        tampered[15] ^= 0xFF;

        assertThrows(IllegalStateException.class,
                () -> service.decrypt(tampered, encrypted.keyVersion()),
                "GCM authentication tag must detect any bit-flip in the ciphertext");
    }

    // ── Wrong key ─────────────────────────────────────────────────────────

    @Test
    void decryptWithWrongKeyVersion_throwsIllegalState() {
        PiiCryptoService.EncryptedValue encryptedWithV1 = service.encrypt("cpf-data");

        // v2 key is different — decryption with the wrong key must fail
        assertThrows(IllegalStateException.class,
                () -> service.decrypt(encryptedWithV1.cipherText(), "v2"),
                "using the wrong key version must cause AES-GCM authentication failure");
    }

    // ── Rotation ──────────────────────────────────────────────────────────

    @Test
    void rotate_producesNewCiphertextDecryptableWithActiveKey() {
        // Encrypt with v1
        PiiCryptoService.EncryptedValue original = service.encrypt("cpf-to-rotate");
        assertEquals("v1", original.keyVersion());

        // Rotate (re-encrypts under v1 since v1 is still active)
        PiiCryptoService.EncryptedValue rotated = service.rotate(original.cipherText(), "v1");

        // Should still be decryptable
        String plainAfterRotation = service.decrypt(rotated.cipherText(), rotated.keyVersion());
        assertEquals("cpf-to-rotate", plainAfterRotation,
                "rotated ciphertext must decrypt to original plaintext");
    }
}
