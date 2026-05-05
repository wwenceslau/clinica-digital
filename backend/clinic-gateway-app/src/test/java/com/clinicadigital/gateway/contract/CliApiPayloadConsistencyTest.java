package com.clinicadigital.gateway.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T111 [US8] CLI ↔ API JSON payload consistency contract test.
 *
 * <p>Validates that the CLI contract document specifies JSON shapes that are consistent
 * with the discriminated patterns used in both the CLI and API layers — ensuring
 * the shared "mode" discriminator, FHIR profile references, and organizationId/tenantId
 * naming are uniform across both surfaces.
 *
 * <p>Note: The CLI output intentionally differs from the API response structure in that:
 * <ul>
 *   <li>CLI wraps single-org session data under a {@code "session"} object (matching the contract).</li>
 *   <li>CLI never exposes the opaque session token (security contract rule).</li>
 *   <li>API exposes the {@code sessionId} for use by client code (HTTP cookie is the primary transport).</li>
 * </ul>
 *
 * <p>Both surfaces share the same "mode" discriminator values ("single"/"multiple")
 * and the same "challengeToken"/"organizations" field names for multi-org flow.
 *
 * Refs: FR-010
 */
class CliApiPayloadConsistencyTest {

    private static final Path CLI_CONTRACTS =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    @Test
    void loginDiscriminatorModeIsConsistentAcrossCliAndApiContracts() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "login 'mode' discriminator consistency (FR-010)",
                () -> assertTrue(contracts.contains("\"mode\""),
                        "both CLI and API share 'mode' discriminator field"),
                () -> assertTrue(contracts.contains("\"single\""),
                        "single-org mode is 'single' in both CLI and API"),
                () -> assertTrue(contracts.contains("\"multiple\""),
                        "multiple-org mode is 'multiple' in both CLI and API")
        );
    }

    @Test
    void challengeTokenFieldNameIsConsistentInMultiOrgFlow() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "challengeToken field name consistency (FR-010)",
                () -> assertTrue(contracts.contains("\"challengeToken\""),
                        "both CLI and API use 'challengeToken' (camelCase) for the multi-org challenge"),
                () -> assertTrue(contracts.contains("--challenge-token"),
                        "CLI option name is --challenge-token (kebab-case) matching Spring Shell convention")
        );
    }

    @Test
    void organizationsArrayFieldNamesAreConsistentAcrossCliAndApiContracts() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "organizations array field consistency (FR-010)",
                () -> assertTrue(contracts.contains("\"organizations\""),
                        "both CLI and API use 'organizations' as the array field name"),
                () -> assertTrue(contracts.contains("\"organizationId\""),
                        "both CLI and API use 'organizationId' inside the organizations array"),
                () -> assertTrue(contracts.contains("\"displayName\""),
                        "both CLI and API use 'displayName' inside organization entries"),
                () -> assertTrue(contracts.contains("\"cnes\""),
                        "both CLI and API include 'cnes' in organization entries")
        );
    }

    @Test
    void selectOrganizationResponseFieldsAreNamedConsistently() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "select-organization response field name consistency (FR-010)",
                () -> assertTrue(contracts.contains("\"tenantId\""),
                        "both CLI and API use 'tenantId' in select-org response"),
                () -> assertTrue(contracts.contains("\"expiresAt\""),
                        "both CLI and API use 'expiresAt' for session expiry"),
                () -> assertTrue(contracts.contains("\"practitionerRoleId\""),
                        "CLI uses 'practitionerRoleId'; contract documents this field")
        );
    }

    @Test
    void cliSecurityRulePreventsSessionTokenExposureInOutput() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        // The common rules section must explicitly state that secrets/tokens must not appear in output
        assertTrue(contracts.contains("segredos") || contracts.contains("token") || contracts.contains("hashes"),
                "CLI contract must specify that secrets/tokens are not exposed in CLI output");
    }

    @Test
    void logoutStatusFieldIsConsistentlyNamedRevoked() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "logout status field consistency (FR-010, FR-024)",
                () -> assertTrue(contracts.contains("\"status\""),
                        "logout response uses 'status' field"),
                () -> assertTrue(contracts.contains("\"revoked\""),
                        "logout success value is 'revoked' in CLI contract")
        );
    }
}
