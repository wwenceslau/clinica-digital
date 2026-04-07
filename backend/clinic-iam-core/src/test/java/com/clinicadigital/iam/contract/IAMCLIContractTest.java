package com.clinicadigital.iam.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T069 [US2] Contract test for iam-core CLI commands.
 */
class IAMCLIContractTest {

    private static final Path CLI_CONTRACTS = Path.of("../../specs/002-definir-fundacao-modular/contracts/cli-contracts.md")
            .toAbsolutePath().normalize();

    @Test
    void iamCliMustDefineLoginLogoutWhoamiAndJson() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "iam-core CLI contract (FR-004, Art. II, Art. XXII)",
                () -> assertTrue(contracts.contains("auth login"), "missing auth login command contract"),
                () -> assertTrue(contracts.contains("auth logout"), "missing auth logout command contract"),
                () -> assertTrue(contracts.contains("auth whoami"), "missing auth whoami command contract"),
                () -> assertTrue(contracts.contains("--json"), "iam commands must support --json"),
                () -> assertTrue(contracts.contains("\"trace_id\""), "contract must expose trace_id"),
                () -> assertTrue(contracts.contains("\"operation\""), "contract must expose operation"),
                () -> assertTrue(contracts.contains("\"outcome\""), "contract must expose outcome")
        );
    }
}
