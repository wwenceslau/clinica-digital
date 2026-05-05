package com.clinicadigital.gateway.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T107 [P] [US8] CLI contract test for the {@code create-tenant-admin} and
 * {@code bootstrap-super-user} commands as part of the US8 CLI integration surface.
 *
 * <p>Verifies that the CLI contract document contains the correct shapes for both
 * administrative provisioning commands — ensuring the full CLI surface is contractually
 * specified and that both commands produce JSON-valid, FHIR-compliant output.
 *
 * Refs: FR-010, FR-003
 */
class CliBootstrapTenantAdminContractTest {

    private static final Path CLI_CONTRACTS =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    // ── bootstrap-super-user ─────────────────────────────────────────────────

    @Test
    void bootstrapCommandIsFullyDefinedWithOptionsAndSuccessShape() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "bootstrap-super-user US8 contract completeness (FR-010)",
                () -> assertTrue(contracts.contains("bootstrap-super-user"),
                        "contract must define bootstrap-super-user command"),
                () -> assertTrue(contracts.contains("--email"),
                        "bootstrap must define --email option"),
                () -> assertTrue(contracts.contains("--password"),
                        "bootstrap must define --password option"),
                () -> assertTrue(contracts.contains("--name"),
                        "bootstrap must define --name option"),
                () -> assertTrue(contracts.contains("\"status\": \"created\"")
                                || contracts.contains("\"status\":"),
                        "success JSON must include 'status' field"),
                () -> assertTrue(contracts.contains("\"profile\""),
                        "success JSON must include 'profile' field for super-user"),
                () -> assertTrue(contracts.contains("\"auditEventId\""),
                        "success JSON must include 'auditEventId' for audit trail")
        );
    }

    @Test
    void bootstrapFhirRndsComplianceInSuccessOutput() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "bootstrap-super-user FHIR/RNDS compliance (FR-010, FR-020)",
                () -> assertTrue(contracts.contains("BRProfissionalSaude"),
                        "bootstrap success JSON must reference RNDS BRProfissionalSaude profile"),
                () -> assertTrue(contracts.contains("meta"),
                        "success JSON must include 'meta' with RNDS profile reference"),
                () -> assertTrue(contracts.contains("\"practitionerId\""),
                        "success JSON must include 'practitionerId' for the created Practitioner")
        );
    }

    @Test
    void bootstrapDuplicateErrorMustBeOperationOutcome() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "bootstrap duplicate OperationOutcome contract (FR-009)",
                () -> assertTrue(contracts.contains("\"resourceType\": \"OperationOutcome\""),
                        "error JSON must be OperationOutcome"),
                () -> assertTrue(contracts.contains("\"code\": \"conflict\"") || contracts.contains("conflict"),
                        "duplicate bootstrap error must use 'conflict' code"),
                () -> assertTrue(contracts.contains("Super-user already exists"),
                        "conflict message must identify existing super-user")
        );
    }

    // ── create-tenant-admin ──────────────────────────────────────────────────

    @Test
    void createTenantAdminCommandIsFullyDefinedWithOptionsAndSuccessShape() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "create-tenant-admin US8 contract completeness (FR-010, FR-003)",
                () -> assertTrue(contracts.contains("create-tenant-admin"),
                        "contract must define create-tenant-admin command"),
                () -> assertTrue(contracts.contains("--cnes"),
                        "create-tenant-admin must define --cnes option"),
                () -> assertTrue(contracts.contains("--admin-email"),
                        "create-tenant-admin must define --admin-email option"),
                () -> assertTrue(contracts.contains("--admin-cpf"),
                        "create-tenant-admin must define --admin-cpf option"),
                () -> assertTrue(contracts.contains("--admin-password"),
                        "create-tenant-admin must define --admin-password option"),
                () -> assertTrue(contracts.contains("\"tenantId\""),
                        "success JSON must include 'tenantId'"),
                () -> assertTrue(contracts.contains("\"adminPractitionerId\""),
                        "success JSON must include 'adminPractitionerId'")
        );
    }

    @Test
    void createTenantAdminFhirRndsOrganizationAndPractitionerInOutput() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "create-tenant-admin FHIR/RNDS output compliance (FR-010, FR-020, FR-022)",
                () -> assertTrue(contracts.contains("BREstabelecimentoSaude"),
                        "organization output must reference RNDS BREstabelecimentoSaude profile"),
                () -> assertTrue(contracts.contains("BRProfissionalSaude"),
                        "admin practitioner output must reference RNDS BRProfissionalSaude profile"),
                () -> assertTrue(contracts.contains("https://saude.gov.br/sid/cnes"),
                        "organization identifiers must use CNES system URL"),
                () -> assertTrue(contracts.contains("https://saude.gov.br/sid/cpf"),
                        "practitioner identifiers must use CPF system URL"),
                () -> assertTrue(contracts.contains("\"organization\""),
                        "success JSON must include 'organization' object"),
                () -> assertTrue(contracts.contains("\"adminPractitioner\""),
                        "success JSON must include 'adminPractitioner' object")
        );
    }

    @Test
    void createTenantAdminConflictMustBeOperationOutcome() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "create-tenant-admin conflict OperationOutcome contract (FR-009)",
                () -> assertTrue(contracts.contains("\"resourceType\": \"OperationOutcome\""),
                        "error JSON must be OperationOutcome"),
                () -> assertTrue(contracts.contains("duplicidade") || contracts.contains("conflict"),
                        "conflict error must reference duplicate/conflict scenario")
        );
    }

    // ── common rules ─────────────────────────────────────────────────────────

    @Test
    void commonContractRulesMustBePresent() throws IOException {
        String contracts = Files.readString(CLI_CONTRACTS);

        assertAll(
                "Common CLI contract rules (FR-009, FR-010)",
                () -> assertTrue(contracts.contains("OperationOutcome"),
                        "common rules must specify OperationOutcome for all errors"),
                () -> assertTrue(contracts.contains("JSON"),
                        "common rules must mandate JSON output"),
                () -> assertTrue(contracts.contains("auditEventId"),
                        "common rules or success shapes must reference auditEventId for traceability"),
                () -> assertTrue(contracts.contains("segredos") || contracts.contains("hashes") || contracts.contains("token"),
                        "common rules must mention opaque token handling")
        );
    }
}
