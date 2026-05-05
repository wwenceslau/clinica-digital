package com.clinicadigital.iam.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExternalSecretEncryptionKeyProvider implements EncryptionKeyProvider {

    private final String activeVersion;
    private final Map<String, String> byVersion = new ConcurrentHashMap<>();

    public ExternalSecretEncryptionKeyProvider(
            @Value("${iam.pii.key.active-version:v1}") String activeVersion,
            @Value("${iam.pii.key.v1:change-me-in-secret-manager}") String v1,
            @Value("${iam.pii.key.v2:}") String v2,
            @Value("${iam.pii.key.v3:}") String v3) {
        this.activeVersion = activeVersion;
        byVersion.put("v1", v1);
        if (v2 != null && !v2.isBlank()) {
            byVersion.put("v2", v2);
        }
        if (v3 != null && !v3.isBlank()) {
            byVersion.put("v3", v3);
        }
    }

    @Override
    public String activeKeyVersion() {
        return activeVersion;
    }

    @Override
    public String keyMaterialForVersion(String version) {
        String value = byVersion.get(version);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing encryption key material for version: " + version);
        }
        return value;
    }
}
