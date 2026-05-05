package com.clinicadigital.gateway.cli;

import com.clinicadigital.iam.application.AuthChallengeService;
import com.clinicadigital.iam.application.AuthenticationService;
import com.clinicadigital.shared.api.TraceContext;
import org.slf4j.MDC;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.UUID;

/**
 * T109 [US8] Spring Shell command: {@code select-organization}.
 *
 * <p>Finalizes authentication in multi-org mode by consuming the challenge token
 * returned by the {@code login} command and selecting a specific organization.
 * On success, a session is created and stored in {@link CliSessionStore}; output
 * contains metadata only (session token is never printed).
 *
 * <p>Contract: {@code specs/004-institution-iam-auth-integration/contracts/cli-contracts.md}
 *
 * Refs: FR-010
 */
@ShellComponent
public class SelectOrganizationCommand {

    private final AuthenticationService authenticationService;
    private final CliSessionStore cliSessionStore;

    public SelectOrganizationCommand(AuthenticationService authenticationService,
                                     CliSessionStore cliSessionStore) {
        this.authenticationService = authenticationService;
        this.cliSessionStore = cliSessionStore;
    }

    /**
     * Finalize multi-org authentication by selecting an organization.
     *
     * @param challengeToken opaque challenge token from the {@code login --mode multiple} response
     * @param organizationId UUID of the organization to authenticate against
     * @return JSON string per CLI contract
     */
    @ShellMethod(key = "select-organization", value = "Finalize multi-org login by selecting an organization")
    public String selectOrganization(
            @ShellOption(value = "--challenge-token", help = "Challenge token from login (required)") String challengeToken,
            @ShellOption(value = "--organization-id", help = "Organization UUID to authenticate against (required)") String organizationId
    ) {
        String traceId = resolveTraceId();

        if (challengeToken == null || challengeToken.isBlank()) {
            return buildOperationOutcome("error", "invalid", "--challenge-token is required");
        }
        if (organizationId == null || organizationId.isBlank()) {
            return buildOperationOutcome("error", "invalid", "--organization-id is required");
        }

        UUID orgId;
        try {
            orgId = UUID.fromString(organizationId.trim());
        } catch (IllegalArgumentException ex) {
            return buildOperationOutcome("error", "invalid", "--organization-id must be a valid UUID");
        }

        try {
            AuthenticationService.LoginResult result =
                    authenticationService.selectOrganization(challengeToken, orgId, traceId);

            cliSessionStore.store(result.sessionId(), result.tenantId());

            return "{\n" +
                   "  \"status\": \"authenticated\",\n" +
                   "  \"expiresAt\": \"" + result.expiresAt() + "\",\n" +
                   "  \"tenantId\": \"" + result.tenantId() + "\",\n" +
                   "  \"practitionerRoleId\": \"" + result.userId() + "\"\n" +
                   "}";

        } catch (AuthChallengeService.AuthChallengeException ex) {
            return buildOperationOutcome("error", "security", sanitize(ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return buildOperationOutcome("error", "invalid", sanitize(ex.getMessage()));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String resolveTraceId() {
        String current = MDC.get("trace_id");
        if (current != null && !current.isBlank()) {
            return current;
        }
        return TraceContext.generate().traceId();
    }

    private String buildOperationOutcome(String severity, String code, String diagnostics) {
        return "{\n" +
               "  \"resourceType\": \"OperationOutcome\",\n" +
               "  \"issue\": [\n" +
               "    {\n" +
               "      \"severity\": \"" + severity + "\",\n" +
               "      \"code\": \"" + code + "\",\n" +
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
                    .replace("\r", "");
    }
}
