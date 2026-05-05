package com.clinicadigital.gateway.cli;

import com.clinicadigital.iam.application.BootstrapSuperUserService;
import com.clinicadigital.iam.application.SuperUserAlreadyExistsException;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * T025 [US1] Spring Shell command: {@code bootstrap-super-user}.
 *
 * <p>Creates the first super-user (profile 0) in an empty system.
 * Idempotent-safe: a second call returns an OperationOutcome conflict error.
 *
 * <p>Contract (from {@code specs/004-institution-iam-auth-integration/contracts/cli-contracts.md}):
 * <pre>
 *   bootstrap-super-user --email &lt;email&gt; --password &lt;secret&gt; --name &lt;text&gt;
 * </pre>
 *
 * <p>Exit: success JSON or OperationOutcome JSON on error.
 *
 * Refs: FR-001, FR-002, FR-009, FR-010 (RBAC: iam.super.bootstrap — CLI-only, no auth required)
 */
@ShellComponent
public class BootstrapSuperUserCommand {

    private static final String RNDS_PRACTITIONER_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude";

    private final BootstrapSuperUserService bootstrapService;

    public BootstrapSuperUserCommand(BootstrapSuperUserService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    /**
     * Bootstrap the super-user.
     *
     * @param email    email address for the super-user account
     * @param password plaintext password (will be hashed before storage)
     * @param name     display name for the super-user Practitioner record
     * @return structured JSON per CLI contract
     */
    @ShellMethod(key = "bootstrap-super-user", value = "Bootstrap the super-user (profile 0) — run once on an empty database")
    public String bootstrapSuperUser(
            @ShellOption(value = "--email", help = "Email address for the super-user (required)") String email,
            @ShellOption(value = "--password", help = "Password for the super-user (required)") String password,
            @ShellOption(value = "--name", help = "Display name for the super-user (required)") String name
    ) {
        // Super-user bootstrap runs in system tenant context
        TenantContextStore.set(TenantContext.from(BootstrapSuperUserService.SYSTEM_TENANT_ID));
        try {
            BootstrapSuperUserService.BootstrapResult result =
                    bootstrapService.bootstrap(email, password, name);

            return "{\n" +
                   "  \"status\": \"created\",\n" +
                   "  \"profile\": 0,\n" +
                   "  \"practitionerId\": \"" + result.practitionerId() + "\",\n" +
                   "  \"meta\": {\n" +
                   "    \"profile\": [\"" + RNDS_PRACTITIONER_PROFILE + "\"]\n" +
                   "  },\n" +
                   "  \"auditEventId\": \"" + result.auditEventId() + "\"\n" +
                   "}";

        } catch (SuperUserAlreadyExistsException ex) {
            return buildConflictOperationOutcome(ex.getMessage());

        } catch (IllegalArgumentException ex) {
            return buildValidationOperationOutcome(ex.getMessage());

        } finally {
            TenantContextStore.clear();
        }
    }

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

    /** Escapes JSON special characters in user-supplied diagnostic strings. */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", " ");
    }
}
