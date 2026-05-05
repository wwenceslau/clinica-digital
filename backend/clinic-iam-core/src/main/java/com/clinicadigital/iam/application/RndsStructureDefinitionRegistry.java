package com.clinicadigital.iam.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RndsStructureDefinitionRegistry {

    private static final String MANIFEST_PATH = "rnds/structure-definitions.json";

    private final Set<String> supportedProfiles;

    public RndsStructureDefinitionRegistry(ObjectMapper objectMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource(MANIFEST_PATH);
        try (InputStream input = resource.getInputStream()) {
            Map<String, List<String>> json = objectMapper.readValue(input, new TypeReference<>() {
            });
            this.supportedProfiles = Set.copyOf(json.getOrDefault("profiles", List.of()));
        }
    }

    public boolean isSupported(String profileUri) {
        if (profileUri == null || profileUri.isBlank()) {
            return false;
        }
        return supportedProfiles.contains(profileUri);
    }

    public void assertSupported(String profileUri) {
        if (!isSupported(profileUri)) {
            throw new IllegalArgumentException("unsupported RNDS StructureDefinition profile: " + profileUri);
        }
    }

    public Set<String> profiles() {
        return supportedProfiles;
    }
}
