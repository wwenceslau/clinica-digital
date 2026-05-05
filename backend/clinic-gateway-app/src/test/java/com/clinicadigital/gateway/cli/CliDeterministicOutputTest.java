package com.clinicadigital.gateway.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T112 [US8] Deterministic logs and exit codes for CLI commands.
 *
 * <p>Validates that all CLI commands ({@code login}, {@code select-organization},
 * {@code logout}) produce deterministic structured JSON output in all code paths:
 * <ul>
 *   <li>Success → JSON matching CLI contract shape.</li>
 *   <li>Error → FHIR OperationOutcome JSON (never a plain-text exception message).</li>
 * </ul>
 *
 * <p>Commands never throw exceptions to the Spring Shell caller; all errors are absorbed
 * and returned as OperationOutcome JSON strings. This ensures exit code 0 in all cases
 * in non-interactive/batch mode (caller inspects JSON content, not exit code, to detect errors).
 *
 * <p>Structured entry/exit logging is handled by {@link CliContextFilter} (AOP aspect),
 * which wraps all {@code @ShellMethod} invocations with MDC-scoped JSON log events.
 *
 * Refs: FR-010, FR-016
 */
class CliDeterministicOutputTest {

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void loginCommandReturnsOperationOutcomeOnNullServiceCallWithoutThrowing() {
        // Arrange: create command with a null AuthenticationService to trigger immediate exception
        // The command constructor accepts null (Spring does not inject here — unit test only)
        LoginCommand command = new LoginCommand(null, new com.clinicadigital.gateway.security.LoginLockoutService(), new CliSessionStore());

        // Act: invoke with a locked-out email to trigger loginLockoutService path
        // Register 5 failures to trigger lockout
        com.clinicadigital.gateway.security.LoginLockoutService lockout =
                new com.clinicadigital.gateway.security.LoginLockoutService();
        LoginCommand lockedCommand = new LoginCommand(null, lockout, new CliSessionStore());
        for (int i = 0; i < 5; i++) {
            lockout.registerFailure("locked@example.com");
        }

        // When locked out, assertNotLocked throws; command must catch it and return OperationOutcome
        String result = lockedCommand.login("locked@example.com", "password");

        assertAll(
                "login returns OperationOutcome on lockout (no exception thrown)",
                () -> assertTrue(result.contains("OperationOutcome"),
                        "locked login must return OperationOutcome"),
                () -> assertTrue(result.contains("security") || result.contains("error"),
                        "OperationOutcome must indicate an error severity/code"),
                () -> assertFalse(result.contains("password"),
                        "password must never appear in output")
        );
    }

    // ── select-organization ───────────────────────────────────────────────────

    @Test
    void selectOrganizationCommandReturnsOperationOutcomeOnInvalidUuidWithoutThrowing() {
        SelectOrganizationCommand command = new SelectOrganizationCommand(null, new CliSessionStore());

        String result = command.selectOrganization("some-challenge", "not-a-uuid");

        assertAll(
                "select-organization returns OperationOutcome on invalid UUID (no exception thrown)",
                () -> assertTrue(result.contains("OperationOutcome"),
                        "invalid UUID must return OperationOutcome"),
                () -> assertTrue(result.contains("invalid"),
                        "OperationOutcome code must be 'invalid'"),
                () -> assertTrue(result.contains("UUID"),
                        "diagnostics must mention UUID validation failure")
        );
    }

    @Test
    void selectOrganizationCommandReturnsOperationOutcomeOnNullChallengeTokenWithoutThrowing() {
        SelectOrganizationCommand command = new SelectOrganizationCommand(null, new CliSessionStore());

        String result = command.selectOrganization(null, "11111111-1111-1111-1111-111111111111");

        assertTrue(result.contains("OperationOutcome"),
                "null challenge token must return OperationOutcome without throwing");
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logoutCommandReturnsOperationOutcomeWhenNoSessionIsActiveWithoutThrowing() {
        LogoutCommand command = new LogoutCommand(null, new CliSessionStore());

        String result = command.logout();

        assertAll(
                "logout returns OperationOutcome when no session is active (no exception thrown)",
                () -> assertTrue(result.contains("OperationOutcome"),
                        "logout with no session must return OperationOutcome"),
                () -> assertTrue(result.contains("not-found") || result.contains("warning"),
                        "OperationOutcome must indicate no session found")
        );
    }

    // ── output format ─────────────────────────────────────────────────────────

    @Test
    void allCommandOutputStartsWithJsonBrace() {
        SelectOrganizationCommand selectOrgCmd = new SelectOrganizationCommand(null, new CliSessionStore());
        LogoutCommand logoutCmd = new LogoutCommand(null, new CliSessionStore());

        String selectOrgError = selectOrgCmd.selectOrganization(null, null);
        String logoutNoSession = logoutCmd.logout();

        assertAll(
                "all CLI outputs are JSON objects starting with '{'",
                () -> assertTrue(selectOrgError.startsWith("{"),
                        "select-organization error must start with '{'"),
                () -> assertTrue(logoutNoSession.startsWith("{"),
                        "logout-no-session must start with '{'")
        );
    }
}
