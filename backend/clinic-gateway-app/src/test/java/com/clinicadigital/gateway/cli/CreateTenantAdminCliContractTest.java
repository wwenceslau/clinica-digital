package com.clinicadigital.gateway.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T033 [P] [US2] CLI contract test for the {@code create-tenant-admin} command.
 *
 * Verifies that the CLI contracts markdown file defines the
 * {@code create-tenant-admin} command with:
 * <ul>
 *   <li>All required {@code --option} arguments.</li>
 *   <li>A success JSON output shape containing every field mandated by the spec.</li>
 *   <li>An error JSON output shape using OperationOutcome with code=conflict.</li>
 * </ul>
 *
 * No Spring context needed — this is a pure file-reading contract test.
 * Refs: FR-010, FR-022
 */
class CreateTenantAdminCliContractTest {

    private static final Path CLI_CONTRACT =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    @Test
    void createTenantAdminCommandMustBeDocumented() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertTrue(contracts.contains("create-tenant-admin"),
                "CLI contract must document the create-tenant-admin command");
    }

    @Test
    void allRequiredOptionsAreDefined() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertAll(
                "create-tenant-admin required options (FR-010)",
                () -> assertTrue(contracts.contains("--tenant-name"),
                        "contract must include --tenant-name option"),
                () -> assertTrue(contracts.contains("--cnes"),
                        "contract must include --cnes option"),
                () -> assertTrue(contracts.contains("--admin-display-name"),
                        "contract must include --admin-display-name option"),
                () -> assertTrue(contracts.contains("--admin-email"),
                        "contract must include --admin-email option"),
                () -> assertTrue(contracts.contains("--admin-cpf"),
                        "contract must include --admin-cpf option"),
                () -> assertTrue(contracts.contains("--admin-password"),
                        "contract must include --admin-password option")
        );
    }

    @Test
    void successJsonContainsTenantIdField() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertTrue(contracts.contains("\"tenantId\""),
                "success JSON must contain tenantId field");
    }

    @Test
    void successJsonContainsAdminPractitionerIdField() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertTrue(contracts.contains("\"adminPractitionerId\""),
                "success JSON must contain adminPractitionerId field");
    }

    @Test
    void successJsonContainsOrganizationSection() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertAll(
                "success JSON organization section (FR-022)",
                () -> assertTrue(contracts.contains("\"organization\""),
                        "success JSON must have organization key"),
                () -> assertTrue(contracts.contains("\"displayName\""),
                        "organization section must contain displayName"),
                () -> assertTrue(contracts.contains("\"accountActive\""),
                        "organization section must contain accountActive"),
                () -> assertTrue(contracts.contains("BREstabelecimentoSaude"),
                        "organization section must reference BREstabelecimentoSaude RNDS profile"),
                () -> assertTrue(contracts.contains("https://saude.gov.br/sid/cnes"),
                        "organization section must use CNES identifier system")
        );
    }

    @Test
    void successJsonContainsAdminPractitionerSection() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertAll(
                "success JSON adminPractitioner section (FR-022)",
                () -> assertTrue(contracts.contains("\"adminPractitioner\""),
                        "success JSON must have adminPractitioner key"),
                () -> assertTrue(contracts.contains("BRProfissionalSaude"),
                        "adminPractitioner section must reference BRProfissionalSaude RNDS profile"),
                () -> assertTrue(contracts.contains("https://saude.gov.br/sid/cpf"),
                        "adminPractitioner section must use CPF identifier system")
        );
    }

    @Test
    void successJsonContainsAuditEventId() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertTrue(contracts.contains("\"auditEventId\""),
                "success JSON must contain auditEventId field (FR-022)");
    }

    @Test
    void errorResponseIsOperationOutcomeWithConflictCode() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertAll(
                "error JSON must be OperationOutcome with code=conflict (FR-009)",
                () -> assertTrue(contracts.contains("\"resourceType\": \"OperationOutcome\""),
                        "error JSON resourceType must be OperationOutcome"),
                () -> assertTrue(contracts.contains("conflict"),
                        "error OperationOutcome must use code=conflict for CNES/name/email conflicts")
        );
    }
}
