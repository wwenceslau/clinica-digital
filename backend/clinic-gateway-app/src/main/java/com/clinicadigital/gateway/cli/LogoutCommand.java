package com.clinicadigital.gateway.cli;

import com.clinicadigital.iam.application.SessionManager;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

/**
 * T110 [US8] Spring Shell command: {@code logout}.
 *
 * <p>Revokes the active CLI session stored by {@link CliSessionStore} (populated by
 * {@code login} or {@code select-organization}). Clears the session store on success.
 *
 * <p>If no session is active, returns an OperationOutcome explaining that no session
 * is present rather than throwing an exception.
 *
 * <p>Contract: {@code specs/004-institution-iam-auth-integration/contracts/cli-contracts.md}
 * Expected success: {@code {"status":"revoked"}}
 *
 * Refs: FR-010, FR-024
 */
@ShellComponent
public class LogoutCommand {

    private final SessionManager sessionManager;
    private final CliSessionStore cliSessionStore;

    public LogoutCommand(SessionManager sessionManager, CliSessionStore cliSessionStore) {
        this.sessionManager = sessionManager;
        this.cliSessionStore = cliSessionStore;
    }

    /**
     * Revoke the currently stored CLI session.
     *
     * @return JSON string per CLI contract
     */
    @ShellMethod(key = "logout", value = "Revoke the active CLI session")
    public String logout() {
        if (!cliSessionStore.hasSession()) {
            return buildOperationOutcome("warning", "not-found", "No active session to revoke");
        }

        java.util.UUID sessionId = cliSessionStore.getSessionId();
        java.util.UUID tenantId = cliSessionStore.getTenantId();

        try {
            sessionManager.revokeSession(sessionId, tenantId);
        } catch (Exception ex) {
            // Best-effort: clear local session state regardless, matching HTTP logout behavior
        }

        cliSessionStore.clear();

        return "{\n" +
               "  \"status\": \"revoked\"\n" +
               "}";
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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

    /** Escapes JSON special characters in diagnostic strings. */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", "");
    }
}
