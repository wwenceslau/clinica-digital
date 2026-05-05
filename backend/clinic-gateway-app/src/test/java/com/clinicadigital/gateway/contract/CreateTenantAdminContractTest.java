package com.clinicadigital.gateway.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T030 [P] [US2] Contract tests for POST /api/admin/tenants endpoint and
 * the {@code create-tenant-admin} CLI command.
 *
 * Verifies that:
 * <ul>
 *   <li>The OpenAPI contract defines {@code POST /api/admin/tenants} with the correct
 *       request/response schema (CreateTenantAdminRequest / CreateTenantAdminResponse).</li>
 *   <li>The CLI contract defines the {@code create-tenant-admin} command with all required
 *       options and the success JSON shape mandated by the spec.</li>
 * </ul>
 *
 * Refs: FR-003, FR-009, FR-010, FR-022
 */
class CreateTenantAdminContractTest {

    private static final Path API_CONTRACT =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/api-openapi.yaml")
                    .toAbsolutePath()
                    .normalize();

    private static final Path CLI_CONTRACT =
            Path.of("../../specs/004-institution-iam-auth-integration/contracts/cli-contracts.md")
                    .toAbsolutePath()
                    .normalize();

    // ── OpenAPI contract ──────────────────────────────────────────────────────

    @Test
    void openApiContractMustDefinePostAdminTenantsEndpoint() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "POST /api/admin/tenants OpenAPI contract (FR-003, FR-022)",
                () -> assertTrue(yaml.contains("/api/admin/tenants"),
                        "contract must define /api/admin/tenants path"),
                () -> assertTrue(yaml.contains("createTenantAdmin"),
                        "contract must define operationId createTenantAdmin"),
                () -> assertTrue(yaml.contains("CreateTenantAdminRequest"),
                        "contract must reference CreateTenantAdminRequest schema")
        );
    }

    @Test
    void openApiResponseSchemaMustContainRequiredFields() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "CreateTenantAdminResponse schema shape (FR-003, FR-022)",
                () -> assertTrue(yaml.contains("CreateTenantAdminResponse"),
                        "contract must define CreateTenantAdminResponse schema"),
                () -> assertTrue(yaml.contains("tenantId"),
                        "response must contain tenantId field"),
                () -> assertTrue(yaml.contains("adminPractitionerId"),
                        "response must contain adminPractitionerId field"),
                () -> assertTrue(yaml.contains("OrganizationSummary"),
                        "response must reference OrganizationSummary"),
                () -> assertTrue(yaml.contains("PractitionerSummary"),
                        "response must reference PractitionerSummary")
        );
    }

    @Test
    void openApiMustDocumentConflictResponse() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "POST /api/admin/tenants 409 conflict response (FR-009)",
                () -> assertTrue(yaml.contains("'409'") || yaml.contains("409"),
                        "contract must document 409 conflict response"),
                () -> assertTrue(yaml.contains("OperationOutcome"),
                        "conflict response must use OperationOutcome schema")
        );
    }

    @Test
    void openApiRequestMustRequireOrganizationAndAdminPractitioner() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "CreateTenantAdminRequest required fields (FR-003)",
                () -> assertTrue(yaml.contains("OrganizationCreateInput"),
                        "request must reference OrganizationCreateInput"),
                () -> assertTrue(yaml.contains("PractitionerCreateInput"),
                        "request must reference PractitionerCreateInput"),
                () -> assertTrue(yaml.contains("cnes"),
                        "OrganizationCreateInput must contain cnes field"),
                () -> assertTrue(yaml.contains("identifiers"),
                        "request must include FHIR identifiers field")
        );
    }

    @Test
    void openApiContractMustDefineUpdateAndDeleteAdminTenantEndpoints() throws IOException {
        String yaml = Files.readString(API_CONTRACT);

        assertAll(
                "PUT/DELETE /api/admin/tenants/{tenantId} OpenAPI contract (FR-003)",
                () -> assertTrue(yaml.contains("/api/admin/tenants/{tenantId}"),
                        "contract must define /api/admin/tenants/{tenantId} path"),
                () -> assertTrue(yaml.contains("operationId: updateTenant"),
                        "contract must define operationId updateTenant"),
                () -> assertTrue(yaml.contains("operationId: deleteTenant"),
                        "contract must define operationId deleteTenant"),
                () -> assertTrue(yaml.contains("UpdateTenantRequest"),
                        "contract must define UpdateTenantRequest schema"),
                () -> assertTrue(yaml.contains("TenantSummaryResponse"),
                        "contract must define TenantSummaryResponse schema"),
                () -> assertTrue(yaml.contains("'204'"),
                        "delete operation must document 204 no-content response")
        );
    }

    // ── CLI contract ──────────────────────────────────────────────────────────

    @Test
    void cliContractMustDefineCreateTenantAdminCommand() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertAll(
                "create-tenant-admin CLI contract (FR-010)",
                () -> assertTrue(contracts.contains("create-tenant-admin"),
                        "CLI contract must define create-tenant-admin command"),
                () -> assertTrue(contracts.contains("--cnes"),
                        "CLI command must include --cnes option"),
                () -> assertTrue(contracts.contains("--admin-email"),
                        "CLI command must include --admin-email option"),
                () -> assertTrue(contracts.contains("--admin-cpf"),
                        "CLI command must include --admin-cpf option"),
                () -> assertTrue(contracts.contains("--admin-password"),
                        "CLI command must include --admin-password option")
        );
    }

    @Test
    void cliSuccessJsonMustContainRequiredFields() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertAll(
                "create-tenant-admin success JSON shape (FR-010, FR-022)",
                () -> assertTrue(contracts.contains("\"tenantId\""),
                        "success JSON must contain tenantId field"),
                () -> assertTrue(contracts.contains("\"adminPractitionerId\""),
                        "success JSON must contain adminPractitionerId field"),
                () -> assertTrue(contracts.contains("\"organization\""),
                        "success JSON must contain organization section"),
                () -> assertTrue(contracts.contains("\"adminPractitioner\""),
                        "success JSON must contain adminPractitioner section"),
                () -> assertTrue(contracts.contains("BREstabelecimentoSaude"),
                        "success JSON must reference BREstabelecimentoSaude RNDS profile"),
                () -> assertTrue(contracts.contains("BRProfissionalSaude"),
                        "success JSON must reference BRProfissionalSaude RNDS profile"),
                () -> assertTrue(contracts.contains("https://saude.gov.br/sid/cnes"),
                        "success JSON must include CNES identifier system"),
                () -> assertTrue(contracts.contains("https://saude.gov.br/sid/cpf"),
                        "success JSON must include CPF identifier system"),
                () -> assertTrue(contracts.contains("\"auditEventId\""),
                        "success JSON must contain auditEventId")
        );
    }

    @Test
    void cliErrorJsonMustBeOperationOutcome() throws IOException {
        String contracts = Files.readString(CLI_CONTRACT);

        assertAll(
                "create-tenant-admin error JSON shape as OperationOutcome (FR-009)",
                () -> assertTrue(contracts.contains("\"resourceType\": \"OperationOutcome\""),
                        "error JSON must have resourceType=OperationOutcome"),
                () -> assertTrue(contracts.contains("conflict"),
                        "conflict error must use code=conflict")
        );
    }
}
