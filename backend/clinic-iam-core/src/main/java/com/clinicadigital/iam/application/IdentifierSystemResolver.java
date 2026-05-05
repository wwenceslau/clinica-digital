package com.clinicadigital.iam.application;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IdentifierSystemResolver {

    public static final String CNES_SYSTEM = "https://saude.gov.br/sid/cnes";
    public static final String CPF_SYSTEM = "https://saude.gov.br/sid/cpf";

    public String resolve(String identifierType) {
        if (identifierType == null || identifierType.isBlank()) {
            throw new IllegalArgumentException("identifierType must not be blank");
        }
        return switch (identifierType.trim().toLowerCase()) {
            case "cnes" -> CNES_SYSTEM;
            case "cpf" -> CPF_SYSTEM;
            default -> throw new IllegalArgumentException("unsupported identifier type: " + identifierType);
        };
    }

    public Map<String, String> dbToApiToFhirMapping() {
        return Map.of(
                "organizations.cnes", "organization.identifier[system=" + CNES_SYSTEM + "]",
                "practitioners.cpf_encrypted", "practitioner.identifier[system=" + CPF_SYSTEM + "]"
        );
    }
}
