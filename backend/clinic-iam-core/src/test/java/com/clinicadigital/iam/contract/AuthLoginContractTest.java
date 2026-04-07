package com.clinicadigital.iam.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T088 [US3] — Contract test for {@code auth.login} endpoint.
 *
 * <p>Validates that {@code contracts/cli-contracts.md} specifies all required
 * input parameters and output fields for the {@code POST /auth/login} endpoint
 * (and its CLI counterpart). The test is file-based and fails fast if the
 * contract document is missing or under-specified.
 *
 * <p>Contract requirements verified:
 * <ul>
 *   <li>Required input fields: {@code tenant_id}, {@code email}/{@code username},
 *       {@code password}.</li>
 *   <li>Success response fields: {@code session_id}, {@code tenant_id},
 *       {@code user_id}, {@code expires_at}, {@code trace_id}.</li>
 *   <li>Observability fields present: {@code operation}, {@code outcome}.</li>
 *   <li>Error contract defines {@code "code": "forbidden"} when tenant is
 *       missing or invalid (Art. VI — FHIR OperationOutcome).</li>
 *   <li>No reference to any external IdP (Art. XXII: IAM 100% in-app).</li>
 * </ul>
 *
 * Refs: FR-006a, FR-007, FR-010a, Art. VI, Art. XXII
 */
class AuthLoginContractTest {

    private static final Path CLI_CONTRACTS =
            Path.of("../../specs/002-definir-fundacao-modular/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    @Test
    void authLoginContractMustDefineRequiredInputFields() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);

        assertAll(
                "auth.login required input fields (FR-006a)",
                () -> assertTrue(contract.contains("--tenant"), "contract must require --tenant input"),
                () -> assertTrue(contract.contains("--username") || contract.contains("--email"),
                        "contract must require --username or --email input"),
                () -> assertTrue(contract.contains("--password"), "contract must require --password input")
        );
    }

    @Test
    void authLoginContractMustDefineSuccessResponseFields() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);

        assertAll(
                "auth.login success response fields (FR-007, FR-010a)",
                () -> assertTrue(contract.contains("\"session_id\""),
                        "success response must include session_id (FR-007)"),
                () -> assertTrue(contract.contains("\"tenant_id\""),
                        "success response must include tenant_id (Art. 0)"),
                () -> assertTrue(contract.contains("\"user_id\""),
                        "success response must include user_id"),
                () -> assertTrue(contract.contains("\"expires_at\""),
                        "success response must include expires_at (FR-007)"),
                () -> assertTrue(contract.contains("\"trace_id\""),
                        "success response must include trace_id (FR-010a)")
        );
    }

    @Test
    void authLoginContractMustDefineObservabilityFields() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);

        assertAll(
                "auth.login observability fields (Art. XI, FR-010a)",
                () -> assertTrue(contract.contains("\"operation\""),
                        "response must include operation field (Art. XI)"),
                () -> assertTrue(contract.contains("\"outcome\""),
                        "response must include outcome field (Art. XI)")
        );
    }

    @Test
    void authLoginContractMustDefineErrorContractForInvalidTenant() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);

        assertAll(
                "auth.login error contract for invalid/missing tenant (FR-002a, Art. VI)",
                () -> assertTrue(contract.contains("\"severity\""),
                        "error contract must include FHIR severity field (Art. VI)"),
                () -> assertTrue(contract.contains("\"code\""),
                        "error contract must include FHIR code field (Art. VI)"),
                () -> assertTrue(contract.contains("forbidden") || contract.contains("invalid"),
                        "error code must indicate forbidden/invalid tenant context"),
                () -> assertTrue(contract.contains("\"diagnostics\""),
                        "error contract must include diagnostics explaining the failure (Art. VI)")
        );
    }

    @Test
    void authLoginContractMustNotReferenceExternalIdp() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);
        String lowerContract = contract.toLowerCase();

        assertAll(
                        "Art. XXII: IAM MUST be 100% in-app — no external IdP usage references",
                        () -> assertTrue(!lowerContract.contains("keycloak.io"),
                                "contract must not reference Keycloak SDK/URL (Art. XXII)"),
                        () -> assertTrue(!lowerContract.contains("auth0.com"),
                                "contract must not reference Auth0 domain (Art. XXII)"),
                        () -> assertTrue(!lowerContract.contains("okta.com"),
                                "contract must not reference Okta domain (Art. XXII)"),
                        () -> assertTrue(!lowerContract.contains("cognito-idp"),
                                "contract must not reference AWS Cognito endpoint (Art. XXII)"),
                        () -> assertTrue(!lowerContract.contains("login.microsoftonline"),
                                "contract must not reference Azure AD endpoint (Art. XXII)")
        );
    }
}
