package com.clinicadigital.iam.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T089 [US3] — Contract test for IAM CLI commands:
 * {@code auth login}, {@code auth logout}, {@code auth whoami}.
 *
 * <p>Validates that {@code contracts/cli-contracts.md} fully specifies the CLI
 * contract for all three authentication commands, including error scenarios and
 * {@code --json} output support.
 *
 * <p>Contract requirements verified:
 * <ul>
 *   <li>{@code auth login}: required flags, success JSON (session_id, expires_at),
 *       error JSON with FHIR OperationOutcome structure.</li>
 *   <li>{@code auth logout}: required --session-id flag, revoked=true in JSON output.</li>
 *   <li>{@code auth whoami}: returns user_id, tenant_id, roles[] array.</li>
 *   <li>All commands support {@code --json} flag (Art. II).</li>
 *   <li>All responses expose observability fields: trace_id, operation, outcome (Art. XI).</li>
 *   <li>No external IdP references anywhere in the CLI contract (Art. XXII).</li>
 * </ul>
 *
 * Refs: FR-004, FR-006a, FR-007, FR-010a, Art. II, Art. XI, Art. XXII
 */
class AuthCLIContractTest {

    private static final Path CLI_CONTRACTS =
            Path.of("../../specs/002-definir-fundacao-modular/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    @Test
    void authLoginCommandMustBeDocumented() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);
        assertTrue(contract.contains("auth login"),
                "contract must document 'auth login' CLI command (FR-004, Art. II)");
    }

    @Test
    void authLogoutCommandMustBeDocumented() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);
        assertTrue(contract.contains("auth logout"),
                "contract must document 'auth logout' CLI command (FR-004, Art. II)");
    }

    @Test
    void authWhoamiCommandMustBeDocumented() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);
        assertTrue(contract.contains("auth whoami"),
                "contract must document 'auth whoami' CLI command (FR-004, Art. II)");
    }

    @Test
    void authCommandsMustSupportJsonFlag() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);
        assertTrue(contract.contains("--json"),
                "all auth commands must support --json output flag (Art. II)");
    }

    @Test
    void authLoginSuccessResponseMustContainSessionId() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);
        assertTrue(contract.contains("\"session_id\""),
                "auth login success JSON must include session_id token (FR-007)");
    }

    @Test
    void authLoginSuccessResponseMustContainExpiresAt() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);
        assertTrue(contract.contains("\"expires_at\""),
                "auth login success JSON must include expires_at timestamp (FR-007)");
    }

    @Test
    void authLogoutSuccessResponseMustContainRevokedTrue() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);
        assertTrue(contract.contains("\"revoked\""),
                "auth logout success JSON must include revoked field (FR-007)");
    }

    @Test
    void authWhoamiResponseMustContainUserTenantAndRoles() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);

        assertAll(
                "auth whoami response fields (FR-006a)",
                () -> assertTrue(contract.contains("\"user_id\""),
                        "whoami response must include user_id"),
                () -> assertTrue(contract.contains("\"tenant_id\""),
                        "whoami response must include tenant_id (Art. 0)"),
                () -> assertTrue(contract.contains("\"roles\""),
                        "whoami response must include roles array (FR-006)")
        );
    }

    @Test
    void allAuthResponsesMustContainObservabilityFields() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);

        assertAll(
                "observability fields in auth CLI responses (Art. XI, FR-010a)",
                () -> assertTrue(contract.contains("\"trace_id\""),
                        "auth responses must include trace_id (FR-010a)"),
                () -> assertTrue(contract.contains("\"operation\""),
                        "auth responses must include operation field (Art. XI)"),
                () -> assertTrue(contract.contains("\"outcome\""),
                        "auth responses must include outcome field (Art. XI)")
        );
    }

    @Test
    void authErrorContractMustExposeFhirOperationOutcomeStructure() throws IOException {
        String contract = Files.readString(CLI_CONTRACTS);

        assertAll(
                "error contract must follow FHIR OperationOutcome (Art. VI)",
                () -> assertTrue(contract.contains("\"issue\""),
                        "error contract must include top-level 'issue' array (FHIR OperationOutcome)"),
                () -> assertTrue(contract.contains("\"severity\""),
                        "issue entry must include severity field"),
                () -> assertTrue(contract.contains("\"diagnostics\""),
                        "issue entry must include diagnostics string explaining the error")
        );
    }

    @Test
    void authCLIContractMustProhibitExternalIdpReference() throws IOException {
        String lower = Files.readString(CLI_CONTRACTS).toLowerCase();

        assertAll(
                "Art. XXII: no external IdP references in auth CLI contract",
                                () -> assertTrue(!lower.contains("keycloak.io"),         "must not reference Keycloak SDK/URL"),
                                () -> assertTrue(!lower.contains("auth0.com"),           "must not reference Auth0 domain"),
                                () -> assertTrue(!lower.contains("okta.com"),            "must not reference Okta domain"),
                                () -> assertTrue(!lower.contains("cognito-idp"),         "must not reference AWS Cognito endpoint"),
                                () -> assertTrue(!lower.contains("login.microsoftonline"), "must not reference Azure AD endpoint")
        );
    }
}
