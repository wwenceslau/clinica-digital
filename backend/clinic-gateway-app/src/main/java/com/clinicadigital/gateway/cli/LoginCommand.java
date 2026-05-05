package com.clinicadigital.gateway.cli;

import com.clinicadigital.iam.application.AuthChallengeService;
import com.clinicadigital.iam.application.AuthenticationService;
import com.clinicadigital.gateway.security.LoginLockoutService;
import com.clinicadigital.shared.api.TraceContext;
import org.slf4j.MDC;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

/**
 * T109 [US8] Spring Shell command: {@code login}.
 *
 * <p>Authenticates a user by email and password. Returns a discriminated JSON response:
 * <ul>
 *   <li>{@code mode:"single"} — user belongs to exactly one active organization; session is stored
 *       internally in {@link CliSessionStore} (not printed). Output contains metadata only.</li>
 *   <li>{@code mode:"multiple"} — user belongs to multiple orgs; a {@code challengeToken} is
 *       returned for use with {@code select-organization}.</li>
 * </ul>
 *
 * <p>The opaque session token is never printed in CLI output per the CLI contract security rules.
 *
 * <p>Contract: {@code specs/004-institution-iam-auth-integration/contracts/cli-contracts.md}
 *
 * Refs: FR-010
 */
@ShellComponent
public class LoginCommand {

    private final AuthenticationService authenticationService;
    private final LoginLockoutService loginLockoutService;
    private final CliSessionStore cliSessionStore;

    public LoginCommand(AuthenticationService authenticationService,
                        LoginLockoutService loginLockoutService,
                        CliSessionStore cliSessionStore) {
        this.authenticationService = authenticationService;
        this.loginLockoutService = loginLockoutService;
        this.cliSessionStore = cliSessionStore;
    }

    /**
     * Authenticate by email/password and return JSON session metadata.
     *
     * @param email    user email address
     * @param password user password (not echoed, not logged, not printed in output)
     * @return JSON string per CLI contract
     */
    @ShellMethod(key = "login", value = "Authenticate by email and password")
    public String login(
            @ShellOption(value = "--email", help = "User email address (required)") String email,
            @ShellOption(value = "--password", help = "User password (required, not logged)") String password
    ) {
        String traceId = resolveTraceId();

        try {
            loginLockoutService.assertNotLocked(email);

            AuthenticationService.MultiOrgLoginResult result =
                    authenticationService.loginByEmail(email, password, traceId, null, null);

            loginLockoutService.registerSuccess(email);

            if (result instanceof AuthenticationService.MultiOrgLoginResult.SingleOrg singleOrg) {
                cliSessionStore.store(singleOrg.sessionId(), singleOrg.organizationId());
                return buildSingleOrgResponse(singleOrg);
            }

            // MultipleOrgs — no session stored yet; user must call select-organization next
            AuthenticationService.MultiOrgLoginResult.MultipleOrgs multiOrgs =
                    (AuthenticationService.MultiOrgLoginResult.MultipleOrgs) result;
            return buildMultipleOrgsResponse(multiOrgs);

        } catch (IllegalArgumentException ex) {
            // Covers both lockout ("too many login attempts") and validation errors
            loginLockoutService.registerFailure(email);
            return buildOperationOutcome("error", "security", ex.getMessage());
        } catch (AuthenticationService.InvalidCredentialsException ex) {
            loginLockoutService.registerFailure(email);
            return buildOperationOutcome("error", "security", "Invalid credentials");
        }
    }

    // ── JSON builders ─────────────────────────────────────────────────────────

    private String buildSingleOrgResponse(AuthenticationService.MultiOrgLoginResult.SingleOrg singleOrg) {
        return "{\n" +
               "  \"mode\": \"single\",\n" +
               "  \"session\": {\n" +
               "    \"expiresAt\": \"" + singleOrg.expiresAt() + "\",\n" +
               "    \"practitioner\": {\n" +
               "      \"id\": \"" + singleOrg.userId() + "\"\n" +
               "    },\n" +
               "    \"tenant\": {\n" +
               "      \"id\": \"" + singleOrg.organizationId() + "\"\n" +
               "    }\n" +
               "  }\n" +
               "}";
    }

    private String buildMultipleOrgsResponse(AuthenticationService.MultiOrgLoginResult.MultipleOrgs multiOrgs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"mode\": \"multiple\",\n");
        sb.append("  \"challengeToken\": \"").append(sanitize(multiOrgs.challengeToken())).append("\",\n");
        sb.append("  \"organizations\": [\n");

        List<AuthChallengeService.OrganizationOption> orgs = multiOrgs.organizations();
        for (int i = 0; i < orgs.size(); i++) {
            AuthChallengeService.OrganizationOption org = orgs.get(i);
            sb.append("    {\n");
            sb.append("      \"organizationId\": \"").append(org.organizationId()).append("\",\n");
            sb.append("      \"displayName\": \"").append(sanitize(org.displayName())).append("\",\n");
            sb.append("      \"cnes\": \"").append(sanitize(org.cnes())).append("\"\n");
            sb.append("    }");
            if (i < orgs.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
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

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String resolveTraceId() {
        String current = MDC.get("trace_id");
        if (current != null && !current.isBlank()) {
            return current;
        }
        return TraceContext.generate().traceId();
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
