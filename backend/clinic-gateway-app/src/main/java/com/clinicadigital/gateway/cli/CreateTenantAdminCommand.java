package com.clinicadigital.gateway.cli;

import com.clinicadigital.iam.application.AdminEmailAlreadyExistsException;
import com.clinicadigital.iam.application.CreateTenantAdminResult;
import com.clinicadigital.iam.application.CreateTenantAdminService;
import com.clinicadigital.iam.application.TenantAlreadyExistsException;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * T038 [US2] Spring Shell command: {@code create-tenant-admin}.
 *
 * <p>Creates a new tenant together with its Organization (FHIR BREstabelecimentoSaude),
 * admin Practitioner (FHIR BRProfissionalSaude) and admin IamUser (profile=10).
 *
 * <p>Contract (from {@code specs/004-institution-iam-auth-integration/contracts/cli-contracts.md}):
 * <pre>
 *   create-tenant-admin
 *     --tenant-name &lt;text&gt;
 *     --cnes &lt;7digits&gt;
 *     --admin-display-name &lt;text&gt;
 *     --admin-email &lt;email&gt;
 *     --admin-cpf &lt;11digits&gt;
 *     --admin-password &lt;secret&gt;
 * </pre>
 *
 * <p>Exit: success JSON or OperationOutcome JSON on error.
 *
 * Refs: FR-003, FR-009, FR-010, FR-022
 */
@ShellComponent
public class CreateTenantAdminCommand {

    private static final String RNDS_ORG_PROFILE =
            CreateTenantAdminService.RNDS_ORG_PROFILE;
    private static final String RNDS_PRACTITIONER_PROFILE =
            CreateTenantAdminService.RNDS_PRACTITIONER_PROFILE;

    private static final String CNES_SYSTEM = "https://saude.gov.br/sid/cnes";
    private static final String CPF_SYSTEM  = "https://saude.gov.br/sid/cpf";

    private final CreateTenantAdminService createTenantAdminService;

    public CreateTenantAdminCommand(CreateTenantAdminService createTenantAdminService) {
        this.createTenantAdminService = createTenantAdminService;
    }

    /**
     * Provision a new tenant with an organization and admin user in one step.
     *
     * @param tenantName       Display name for the organization (globally unique)
     * @param cnes             7-digit CNES code (globally unique)
     * @param adminDisplayName Full name of the admin practitioner
     * @param adminEmail       Login email for the admin (globally unique for profile=10)
     * @param adminCpf         11-digit CPF of the admin (stored encrypted)
     * @param adminPassword    Plaintext password for the admin (will be BCrypt-hashed)
     * @return structured JSON per CLI contract on success, OperationOutcome on error
     */
    @ShellMethod(
            key = "create-tenant-admin",
            value = "Create a new tenant, organization and admin user (profile 10)"
    )
    public String createTenantAdmin(
            @ShellOption(value = "--tenant-name",
                    help = "Organization display name (required, globally unique)") String tenantName,
            @ShellOption(value = "--cnes",
                    help = "7-digit CNES code (required, globally unique)") String cnes,
            @ShellOption(value = "--admin-display-name",
                    help = "Admin practitioner full name (required)") String adminDisplayName,
            @ShellOption(value = "--admin-email",
                    help = "Admin login email (required, globally unique for admins)") String adminEmail,
            @ShellOption(value = "--admin-cpf",
                    help = "Admin CPF — 11 digits, stored encrypted (required)") String adminCpf,
            @ShellOption(value = "--admin-password",
                    help = "Admin plaintext password — will be BCrypt-hashed (required)") String adminPassword
    ) {
        // Tenant-admin creation runs in the system tenant context (no user session yet)
        TenantContextStore.set(TenantContext.from(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")));
        try {
            CreateTenantAdminResult result = createTenantAdminService.create(
                    tenantName, cnes, adminDisplayName, adminEmail, adminCpf, adminPassword);

            return "{\n" +
                   "  \"status\": \"created\",\n" +
                   "  \"tenantId\": \"" + result.tenantId() + "\",\n" +
                   "  \"adminPractitionerId\": \"" + result.adminPractitionerId() + "\",\n" +
                   "  \"organization\": {\n" +
                   "    \"displayName\": \"" + sanitize(tenantName) + "\",\n" +
                   "    \"accountActive\": true,\n" +
                   "    \"identifiers\": [\n" +
                   "      {\"system\": \"" + CNES_SYSTEM + "\", \"value\": \"" + sanitize(cnes) + "\"}\n" +
                   "    ],\n" +
                   "    \"meta\": {\n" +
                   "      \"profile\": [\"" + RNDS_ORG_PROFILE + "\"]\n" +
                   "    }\n" +
                   "  },\n" +
                   "  \"adminPractitioner\": {\n" +
                   "    \"displayName\": \"" + sanitize(adminDisplayName) + "\",\n" +
                   "    \"accountActive\": true,\n" +
                   "    \"identifiers\": [\n" +
                   "      {\"system\": \"" + CPF_SYSTEM + "\", \"value\": \"" + maskCpf(adminCpf) + "\"}\n" +
                   "    ],\n" +
                   "    \"meta\": {\n" +
                   "      \"profile\": [\"" + RNDS_PRACTITIONER_PROFILE + "\"]\n" +
                   "    }\n" +
                   "  },\n" +
                   "  \"auditEventId\": " + result.auditEventId() + "\n" +
                   "}";

        } catch (TenantAlreadyExistsException ex) {
            return buildConflictOperationOutcome(ex.getMessage());

        } catch (AdminEmailAlreadyExistsException ex) {
            return buildConflictOperationOutcome(ex.getMessage());

        } catch (IllegalArgumentException ex) {
            return buildValidationOperationOutcome(ex.getMessage());

        } finally {
            TenantContextStore.clear();
        }
    }

    // ── Error helpers (same structure as BootstrapSuperUserCommand) ───────────

    private String buildConflictOperationOutcome(String diagnostics) {
        return "{\n" +
               "  \"resourceType\": \"OperationOutcome\",\n" +
               "  \"issue\": [\n" +
               "    {\n" +
               "      \"severity\": \"error\",\n" +
               "      \"code\": \"conflict\",\n" +
               "      \"diagnostics\": \"" + sanitize(diagnostics) + "\"\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }

    private String buildValidationOperationOutcome(String diagnostics) {
        return "{\n" +
               "  \"resourceType\": \"OperationOutcome\",\n" +
               "  \"issue\": [\n" +
               "    {\n" +
               "      \"severity\": \"error\",\n" +
               "      \"code\": \"invalid\",\n" +
               "      \"diagnostics\": \"" + sanitize(diagnostics) + "\"\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }

    /** Escapes JSON special characters in user-supplied strings. */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", " ")
                    .replace("\t", " ");
    }

    /**
     * Masks all but the last 2 digits of a CPF for safe display in JSON output.
     * The real CPF is stored encrypted in the database.
     */
    private static String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 2) return "***";
        return "*".repeat(cpf.length() - 2) + cpf.substring(cpf.length() - 2);
    }
}
