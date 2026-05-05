package com.clinicadigital.gateway.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T040 [P] [US3] Contract tests for POST /api/public/clinic-registration endpoint.
 *
 * Verifies that:
 * <ul>
 *   <li>The OpenAPI contract defines {@code POST /api/public/clinic-registration}
 *       with operationId {@code registerClinic}.</li>
 *   <li>The request schema is {@code CreateTenantAdminRequest}.</li>
 *   <li>The 201 response references {@code CreateTenantAdminResponse}.</li>
 *   <li>The 409 and 400 responses reference {@code OperationOutcome}.</li>
 * </ul>
 *
 * Pure file-reading test — no Spring context required.
 * Refs: FR-003, FR-009, FR-022
 */
class PublicClinicRegistrationContractTest {

    private static final Path API_CONTRACT =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/api-openapi.yaml")
                    .toAbsolutePath()
                    .normalize();

    @Test
    void openApiContractMustDefinePublicClinicRegistrationEndpoint() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "POST /api/public/clinic-registration OpenAPI contract (FR-003, FR-022)",
                () -> assertTrue(yaml.contains("/api/public/clinic-registration"),
                        "contract must define /api/public/clinic-registration path"),
                () -> assertTrue(yaml.contains("registerClinic"),
                        "contract must define operationId registerClinic"),
                () -> assertTrue(yaml.contains("CreateTenantAdminRequest"),
                        "contract must reference CreateTenantAdminRequest schema")
        );
    }

    @Test
    void openApiPublicRegistration201ResponseMustReferenceCreateTenantAdminResponse() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "POST /api/public/clinic-registration 201 response schema (FR-003)",
                () -> assertTrue(yaml.contains("CreateTenantAdminResponse"),
                        "contract must define CreateTenantAdminResponse schema"),
                () -> assertTrue(yaml.contains("tenantId"),
                        "response must contain tenantId field"),
                () -> assertTrue(yaml.contains("adminPractitionerId"),
                        "response must contain adminPractitionerId field")
        );
    }

    @Test
    void openApiPublicRegistrationMustDocumentConflictResponse() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "POST /api/public/clinic-registration 409 conflict response (FR-009)",
                () -> assertTrue(yaml.contains("'409'") || yaml.contains("409"),
                        "contract must document 409 response"),
                () -> assertTrue(yaml.contains("OperationOutcome"),
                        "conflict response must reference OperationOutcome schema"),
                () -> assertTrue(yaml.contains("issue"),
                        "OperationOutcome must include issue field")
        );
    }

    @Test
    void openApiPublicRegistrationMustDocumentValidationError() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "POST /api/public/clinic-registration 400 validation error response (FR-009)",
                () -> assertTrue(yaml.contains("'400'") || yaml.contains("400"),
                        "contract must document 400 response"),
                () -> assertTrue(yaml.contains("Falha de validacao") || yaml.contains("invalid"),
                        "400 response must describe validation failure")
        );
    }

    @Test
    void openApiPublicRegistrationRequestMustRequireOrganizationAndAdminPractitioner() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "CreateTenantAdminRequest required fields (FR-003)",
                () -> assertTrue(yaml.contains("organization"),
                        "request must require organization field"),
                () -> assertTrue(yaml.contains("adminPractitioner"),
                        "request must require adminPractitioner field"),
                () -> assertTrue(yaml.contains("cnes"),
                        "organization must include cnes field")
        );
    }
}
