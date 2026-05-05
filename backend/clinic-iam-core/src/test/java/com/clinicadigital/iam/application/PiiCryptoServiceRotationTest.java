package com.clinicadigital.iam.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PiiCryptoServiceRotationTest {

    @Test
    void keyProviderShouldExposeVersionedKeysForRotationWindow() {
        EncryptionKeyProvider provider = new EncryptionKeyProvider() {
            @Override
            public String activeKeyVersion() {
                return "v2";
            }

            @Override
            public String keyMaterialForVersion(String version) {
                return switch (version) {
                    case "v1" -> "legacy-key";
                    case "v2" -> "new-key";
                    default -> throw new IllegalArgumentException("unknown");
                };
            }
        };

        assertEquals("v2", provider.activeKeyVersion());
        assertEquals("legacy-key", provider.keyMaterialForVersion("v1"));
        assertEquals("new-key", provider.keyMaterialForVersion("v2"));
        assertArrayEquals("legacy-key".getBytes(), provider.keyMaterialForVersion("v1").getBytes());
    }
}
