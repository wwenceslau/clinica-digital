package com.clinicadigital.gateway.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T106 [P] [US8] CLI contract test for the {@code login} and {@code select-organization} commands.
 *
 * <p>Verifies that the CLI contract document
 * {@code specs/004-institution-iam-auth-integration/contracts/cli-contracts.md}
 * defines the correct command shapes, option names, and required JSON output fields
 * for both the login (single/multiple org) and select-organization flows.
 *
 * Refs: FR-010
 */
class CliLoginContractTest {

    private static final Path CLI_CONTRACTS =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    // ── login command ────────────────────────────────────────────────────────

    @Test
    void loginCommandMustBeDefinedInCliContracts() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "login CLI command shape (FR-010)",
                () -> assertTrue(contracts.contains("login --email"),
                        "contract must define login command with --email option"),
                () -> assertTrue(contracts.contains("--password"),
                        "contract must include --password option for login")
        );
    }

    @Test
    void loginSingleOrgSuccessJsonMustContainRequiredFields() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "login single-org success JSON shape (FR-010)",
                () -> assertTrue(contracts.contains("\"mode\""),
                        "success JSON must contain 'mode' discriminator field"),
                () -> assertTrue(contracts.contains("\"single\""),
                        "single-org mode value must be 'single'"),
                () -> assertTrue(contracts.contains("\"session\""),
                        "single-org success JSON must contain 'session' object"),
                () -> assertTrue(contracts.contains("\"expiresAt\""),
                        "session object must contain 'expiresAt' field"),
                () -> assertTrue(contracts.contains("\"practitioner\""),
                        "session object must contain 'practitioner' sub-object"),
                () -> assertTrue(contracts.contains("\"profileType\""),
                        "practitioner must include 'profileType' field"),
                () -> assertTrue(contracts.contains("\"tenant\""),
                        "session must contain 'tenant' sub-object"),
                () -> assertTrue(contracts.contains("\"activePractitionerRole\""),
                        "session must contain 'activePractitionerRole' sub-object")
        );
    }

    @Test
    void loginMultipleOrgSuccessJsonMustContainRequiredFields() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "login multiple-org success JSON shape (FR-010)",
                () -> assertTrue(contracts.contains("\"multiple\""),
                        "multiple-org mode value must be 'multiple'"),
                () -> assertTrue(contracts.contains("\"challengeToken\""),
                        "multiple-org response must contain 'challengeToken'"),
                () -> assertTrue(contracts.contains("\"organizations\""),
                        "multiple-org response must contain 'organizations' array")
        );
    }

    @Test
    void loginErrorJsonMustBeOperationOutcome() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        // Errors in login are covered by the Common Contract Rules section
        assertAll(
                "login error must be OperationOutcome (FR-009)",
                () -> assertTrue(contracts.contains("\"resourceType\": \"OperationOutcome\""),
                        "error JSON must have resourceType=OperationOutcome"),
                () -> assertTrue(contracts.contains("\"severity\""),
                        "OperationOutcome issue must contain 'severity'"),
                () -> assertTrue(contracts.contains("\"code\""),
                        "OperationOutcome issue must contain 'code'")
        );
    }

    // ── select-organization command ──────────────────────────────────────────

    @Test
    void selectOrganizationCommandMustBeDefinedInCliContracts() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "select-organization CLI command shape (FR-010)",
                () -> assertTrue(contracts.contains("select-organization"),
                        "contract must define select-organization command"),
                () -> assertTrue(contracts.contains("--challenge-token"),
                        "contract must include --challenge-token option"),
                () -> assertTrue(contracts.contains("--organization-id"),
                        "contract must include --organization-id option")
        );
    }

    @Test
    void selectOrganizationSuccessJsonMustContainRequiredFields() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "select-organization success JSON shape (FR-010)",
                () -> assertTrue(contracts.contains("\"authenticated\"") || contracts.contains("status"),
                        "success JSON must contain authenticated status"),
                () -> assertTrue(contracts.contains("\"tenantId\""),
                        "success JSON must contain 'tenantId'"),
                () -> assertTrue(contracts.contains("\"practitionerRoleId\""),
                        "success JSON must contain 'practitionerRoleId'")
        );
    }
}
