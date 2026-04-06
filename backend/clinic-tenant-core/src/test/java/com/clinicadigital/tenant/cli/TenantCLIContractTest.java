package com.clinicadigital.tenant.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T042 — Contract test: CLI command `tenant create` requires --tenant flag and
 * follows the defined CLI contract in contracts/cli-contracts.md (FR-002a, Art. II).
 *
 * <p>Verifies that:
 * <ul>
 *   <li>The CLI contract document defines mandatory {@code --tenant} context for iam-core commands.</li>
 *   <li>The {@code tenant create} command contract includes all mandatory fields:
 *       {@code --slug}, {@code --legal-name}, {@code --plan-tier}.</li>
 *   <li>The CLI contract defines structured JSON output with tenant_id, trace_id, operation, outcome.</li>
 *   <li>The error contract uses FHIR OperationOutcome structure with
 *       {@code severity: "error"}, {@code code: "forbidden"} for missing tenant context.</li>
 *   <li>The contract prohibits external IdP dependencies (Art. XXII).</li>
 * </ul>
 *
 * <p><b>TDD state</b>: GREEN from the start — this validates the specification contract.
 * Phase 3.D implementations (T053–T055) must conform to this contract.
 *
 * Refs: FR-002a, FR-004, Art. II, Art. XXII
 */
class TenantCLIContractTest {

    // Resolve cli-contracts.md from the project specs root (relative to repo root)
    private static final Path CLI_CONTRACTS = findCliContracts();

    private static Path findCliContracts() {
        // Try relative paths from working directories that Maven may use
        for (String candidate : new String[]{
                "../../specs/002-definir-fundacao-modular/contracts/cli-contracts.md",
                "../../../specs/002-definir-fundacao-modular/contracts/cli-contracts.md",
                "specs/002-definir-fundacao-modular/contracts/cli-contracts.md"
        }) {
            Path p = Paths.get(candidate).toAbsolutePath().normalize();
            if (Files.exists(p)) {
                return p;
            }
        }
        throw new IllegalStateException("cli-contracts.md not found; run test from repo root or module dir");
    }

    @Test
    void tenantCreateCommandContractMustDefineMandatoryArguments() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "tenant create command must declare all mandatory arguments per Art. II",
                () -> assertTrue(contracts.contains("--slug"),
                        "tenant create MUST declare --slug as mandatory"),
                () -> assertTrue(contracts.contains("--legal-name"),
                        "tenant create MUST declare --legal-name as mandatory"),
                () -> assertTrue(contracts.contains("--plan-tier"),
                        "tenant create MUST declare --plan-tier as mandatory")
        );
    }

    @Test
    void tenantCreateContractMustDefineStructuredJsonOutput() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "tenant create output contract must include observability fields (FR-004, FR-011)",
                () -> assertTrue(contracts.contains("\"tenant_id\""),
                        "output JSON must include tenant_id"),
                () -> assertTrue(contracts.contains("\"trace_id\""),
                        "output JSON must include trace_id for correlation"),
                () -> assertTrue(contracts.contains("\"operation\""),
                        "output JSON must include operation field"),
                () -> assertTrue(contracts.contains("\"outcome\""),
                        "output JSON must include outcome field")
        );
    }

    @Test
    void iamCLIContractMustRequireTenantOnEachCommand() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        // IAM auth commands must require --tenant (FR-002a)
        assertAll(
                "iam-core CLI commands must require --tenant (FR-002a fail-closed)",
                () -> assertTrue(contracts.contains("--tenant"),
                        "IAM auth login MUST declare --tenant as mandatory argument"),
                () -> assertTrue(contracts.contains("auth login"),
                        "iam-core CLI contract must define auth login command")
        );
    }

    @Test
    void cliErrorContractMustUseFhirOperationOutcomeStructure() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "CLI error contract must follow FHIR OperationOutcome structure (Art. VI)",
                () -> assertTrue(contracts.contains("\"severity\""),
                        "error contract must have severity field"),
                () -> assertTrue(contracts.contains("\"code\""),
                        "error contract must have code field"),
                () -> assertTrue(contracts.contains("\"forbidden\""),
                        "error contract must use 'forbidden' code for missing tenant context"),
                () -> assertTrue(contracts.contains("\"diagnostics\""),
                        "error contract must have diagnostics field"),
                () -> assertTrue(contracts.contains("tenant context missing"),
                        "error diagnostics must specify tenant context missing scenario")
        );
    }

    @Test
    void cliContractMustProhibitExternalIdpDependency() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        // Art. XXII: the contract document MUST explicitly prohibit external IdP usage.
        // Provider names may appear in the prohibition text itself — we verify the prohibition keyword.
        assertTrue(contracts.contains("Proibido"),
                "Art. XXII: CLI contract must explicitly state 'Proibido' for external IdP usage");
    }
}
