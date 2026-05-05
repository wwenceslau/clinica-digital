package com.clinicadigital.gateway.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T022 [P] [US1] Contract test for the bootstrap-super-user CLI command.
 *
 * Verifies that the CLI contract defined in
 * specs/004-institution-iam-auth-integration/contracts/cli-contracts.md
 * includes all required shapes for the bootstrap-super-user command.
 *
 * Refs: FR-001, FR-002, FR-010
 */
class BootstrapSuperUserContractTest {

    private static final Path CLI_CONTRACTS =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    @Test
    void bootstrapSuperUserCommandMustBeDefinedInCliContracts() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "bootstrap-super-user CLI contract (FR-001, FR-002, FR-010)",
                () -> assertTrue(contracts.contains("bootstrap-super-user"),
                        "contract must define bootstrap-super-user command"),
                () -> assertTrue(contracts.contains("--email"),
                        "contract must include --email option"),
                () -> assertTrue(contracts.contains("--password"),
                        "contract must include --password option"),
                () -> assertTrue(contracts.contains("--name"),
                        "contract must include --name option")
        );
    }

    @Test
    void bootstrapSuccessJsonMustContainRequiredFields() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "bootstrap-super-user success JSON shape (FR-001, FR-010)",
                () -> assertTrue(contracts.contains("\"status\""),
                        "success JSON must contain 'status' field"),
                () -> assertTrue(contracts.contains("\"profile\""),
                        "success JSON must contain 'profile' field"),
                () -> assertTrue(contracts.contains("\"practitionerId\""),
                        "success JSON must contain 'practitionerId' field"),
                () -> assertTrue(contracts.contains("\"auditEventId\""),
                        "success JSON must contain 'auditEventId' field"),
                () -> assertTrue(contracts.contains("BRProfissionalSaude"),
                        "success JSON must reference BRProfissionalSaude RNDS profile")
        );
    }

    @Test
    void bootstrapErrorJsonMustBeOperationOutcome() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "bootstrap-super-user error JSON shape as OperationOutcome (FR-009)",
                () -> assertTrue(contracts.contains("\"resourceType\": \"OperationOutcome\""),
                        "error JSON must have resourceType=OperationOutcome"),
                () -> assertTrue(contracts.contains("\"severity\""),
                        "OperationOutcome issue must contain 'severity'"),
                () -> assertTrue(contracts.contains("\"code\""),
                        "OperationOutcome issue must contain 'code'"),
                () -> assertTrue(contracts.contains("\"diagnostics\""),
                        "OperationOutcome issue must contain 'diagnostics'"),
                () -> assertTrue(contracts.contains("Super-user already exists"),
                        "conflict diagnostics must describe 'Super-user already exists'")
        );
    }
}
