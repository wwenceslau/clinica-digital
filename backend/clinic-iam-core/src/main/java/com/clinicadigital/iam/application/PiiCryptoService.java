package com.clinicadigital.iam.application;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * T068 [US9] — Symmetric encryption of PII fields (e.g. CPF) using AES-256-GCM.
 *
 * <p>Implementation strategy:
 * <ul>
 *   <li>Algorithm: {@code AES/GCM/NoPadding} — provides authenticated encryption
 *       (confidentiality + integrity + authenticity) as required by FR-011.</li>
 *   <li>Key: SHA-256 of the key-material string → 32 bytes (AES-256).</li>
 *   <li>IV: 12-byte random nonce (GCM recommended), generated per-encrypt.</li>
 *   <li>Auth tag: 128-bit (16 bytes), appended by the JCE to the ciphertext.</li>
 *   <li>Stored format: {@code [12-byte IV][ciphertext + 16-byte tag]}.</li>
 *   <li>Key versioning: managed by {@link EncryptionKeyProvider}; the version is
 *       stored alongside the ciphertext to enable key rotation.</li>
 * </ul>
 *
 * Refs: FR-011, Constitution Art. VI
 */
@Service
public class PiiCryptoService {

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final EncryptionKeyProvider encryptionKeyProvider;
    private final SecureRandom secureRandom;

    public PiiCryptoService(EncryptionKeyProvider encryptionKeyProvider) {
        this.encryptionKeyProvider = encryptionKeyProvider;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypts {@code plainText} with AES-256-GCM using the active key version.
     *
     * @param plainText the sensitive PII value to encrypt (must not be blank)
     * @return an {@link EncryptedValue} containing {@code [IV][ciphertext+tag]} and the key version
     */
    public EncryptedValue encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("plainText must not be blank");
        }
        String version = encryptionKeyProvider.activeKeyVersion();
        SecretKey secretKey = deriveKey(encryptionKeyProvider.keyMaterialForVersion(version));

        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] stored = combineIvAndCipher(iv, cipherBytes);
            return new EncryptedValue(stored, version);
        } catch (Exception e) {
            throw new IllegalStateException("AES-256-GCM encryption failed", e);
        }
    }

    /**
     * Decrypts a {@code cipherText} produced by {@link #encrypt}.
     *
     * @param cipherText the {@code [IV][ciphertext+tag]} byte array
     * @param version    the key version used during encryption
     * @return the original plaintext string
     */
    public String decrypt(byte[] cipherText, String version) {
        if (cipherText == null || cipherText.length <= GCM_IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("cipherText is too short or empty");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        SecretKey secretKey = deriveKey(encryptionKeyProvider.keyMaterialForVersion(version));

        try {
            byte[] iv = Arrays.copyOfRange(cipherText, 0, GCM_IV_LENGTH_BYTES);
            byte[] encryptedBytes = Arrays.copyOfRange(cipherText, GCM_IV_LENGTH_BYTES, cipherText.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(encryptedBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-256-GCM decryption failed — wrong key or corrupt data", e);
        }
    }

    /**
     * Re-encrypts {@code cipherText} under the currently active key, enabling key rotation.
     *
     * @param cipherText     the existing {@code [IV][ciphertext+tag]} payload
     * @param currentVersion the key version used to produce {@code cipherText}
     * @return a new {@link EncryptedValue} under the active key version
     */
    public EncryptedValue rotate(byte[] cipherText, String currentVersion) {
        String plain = decrypt(cipherText, currentVersion);
        return encrypt(plain);
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /**
     * Derives a 256-bit (32-byte) AES key from a String key-material value
     * using SHA-256. This is deterministic, so the same material always produces
     * the same key — required for decryption with the same version.
     */
    static SecretKey deriveKey(String keyMaterial) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private static byte[] combineIvAndCipher(byte[] iv, byte[] cipherBytes) {
        byte[] combined = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);
        return combined;
    }

    /** Carries encrypted PII bytes and the key version used for encryption. */
    public record EncryptedValue(byte[] cipherText, String keyVersion) {
    }
}
