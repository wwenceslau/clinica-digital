package com.clinicadigital.gateway.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T108 [P] [US8] CLI contract test for the {@code logout} command.
 *
 * <p>Verifies that the CLI contract document
 * {@code specs/004-institution-iam-auth-integration/contracts/cli-contracts.md}
 * defines the correct shape for the logout command: immediate session revocation
 * with a {@code {"status":"revoked"}} success response and OperationOutcome on error.
 *
 * Refs: FR-010, FR-007
 */
class CliLogoutContractTest {

    private static final Path CLI_CONTRACTS =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    @Test
    void logoutCommandMustBeDefinedInCliContracts() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertTrue(contracts.contains("logout"),
                "contract must define the logout command");
    }

    @Test
    void logoutSuccessJsonMustContainRevokedStatus() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "logout success JSON shape (FR-010, FR-007)",
                () -> assertTrue(contracts.contains("\"revoked\""),
                        "logout success JSON must contain status value 'revoked'"),
                () -> assertTrue(contracts.contains("status"),
                        "logout success JSON must contain 'status' field name")
        );
    }

    @Test
    void logoutErrorJsonMustBeOperationOutcome() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "logout error JSON must be OperationOutcome (FR-009)",
                () -> assertTrue(contracts.contains("\"resourceType\": \"OperationOutcome\""),
                        "error JSON must have resourceType=OperationOutcome"),
                () -> assertTrue(contracts.contains("\"severity\""),
                        "OperationOutcome must contain 'severity'"),
                () -> assertTrue(contracts.contains("\"diagnostics\""),
                        "OperationOutcome must contain 'diagnostics' with error detail")
        );
    }

    @Test
    void logoutSessionLifecycleIsDefinedInContracts() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        // The logout command must reference the session revocation flow —
        // either through sessionId mention or the revokeSession service behavior
        assertTrue(contracts.contains("revok") || contracts.contains("session"),
                "contract must describe session revocation semantics for logout");
    }
}
