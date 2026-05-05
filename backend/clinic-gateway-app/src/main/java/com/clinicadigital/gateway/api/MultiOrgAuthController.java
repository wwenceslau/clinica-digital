package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.exception.AuthSessionException;
import com.clinicadigital.gateway.security.LoginLockoutService;
import com.clinicadigital.gateway.security.SanitizationValidationGate;
import com.clinicadigital.gateway.security.SessionCookieService;
import com.clinicadigital.iam.application.AuthChallengeService;
import com.clinicadigital.iam.application.AuthenticationService;
import com.clinicadigital.iam.application.SessionManager;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * US4 — Login Multi-Perfil com Seleção de Organização.
 *
 * <p>POST /api/auth/login              — authenticate by email + password (no tenant required)
 * <p>POST /api/auth/select-organization — finalize session after org selection
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class MultiOrgAuthController {

    private static final Duration SESSION_COOKIE_TTL = Duration.ofMinutes(30);

    private final AuthenticationService authenticationService;
    private final SanitizationValidationGate sanitizationValidationGate;
    private final LoginLockoutService loginLockoutService;
    private final SessionCookieService sessionCookieService;
    private final SessionManager sessionManager;

    public MultiOrgAuthController(AuthenticationService authenticationService,
                                   SanitizationValidationGate sanitizationValidationGate,
                                   LoginLockoutService loginLockoutService,
                                   SessionCookieService sessionCookieService,
                                   SessionManager sessionManager) {
        this.authenticationService = authenticationService;
        this.sanitizationValidationGate = sanitizationValidationGate;
        this.loginLockoutService = loginLockoutService;
        this.sessionCookieService = sessionCookieService;
        this.sessionManager = sessionManager;
    }

    /**
     * POST /api/auth/login
     * <p>No X-Tenant-ID required. Returns discriminated response:
     * <ul>
     *   <li>{@code mode: "single"} + session cookie when exactly 1 active org</li>
     *   <li>{@code mode: "multiple"} + challengeToken when 2+ active orgs</li>
     * </ul>
     */
    @PostMapping("/login")
    public ResponseEntity<LoginMultiOrgResponse> login(@RequestBody LoginRequest request,
                                                       HttpServletRequest httpRequest,
                                                       HttpServletResponse httpResponse) {
        if (request == null) {
            throw new IllegalArgumentException("email and password are required");
        }

        String email = sanitizationValidationGate.requireEmail(request.email(), "email");
        String password = sanitizationValidationGate.requirePassword(request.password(), "password");

        loginLockoutService.assertNotLocked(email);

        try {
            AuthenticationService.MultiOrgLoginResult result = authenticationService.loginByEmail(
                    email, password, currentTraceId(), clientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"));

            loginLockoutService.registerSuccess(email);

            if (result instanceof AuthenticationService.MultiOrgLoginResult.SingleOrg singleOrg) {
                sessionCookieService.writeSessionCookie(
                        httpResponse, singleOrg.sessionId(), SESSION_COOKIE_TTL);
                return ResponseEntity.ok(LoginMultiOrgResponse.single(
                        singleOrg.sessionId(),
                        singleOrg.expiresAt().toString(),
                        singleOrg.organizationId(),
                        singleOrg.userId(),
                        currentTraceId()));
            }

            // MultipleOrgs
            AuthenticationService.MultiOrgLoginResult.MultipleOrgs multiOrgs =
                    (AuthenticationService.MultiOrgLoginResult.MultipleOrgs) result;
            List<OrganizationSummary> orgs = multiOrgs.organizations().stream()
                    .map(o -> new OrganizationSummary(o.organizationId(), o.displayName(), o.cnes()))
                    .toList();
            return ResponseEntity.ok(LoginMultiOrgResponse.multiple(
                    multiOrgs.challengeToken(), orgs, currentTraceId()));

        } catch (AuthenticationService.InvalidCredentialsException ex) {
            loginLockoutService.registerFailure(email);
            throw new AuthSessionException(ex.getMessage());
        }
    }

    /**
     * POST /api/auth/select-organization
     * <p>Validates challengeToken, emits session for the chosen organization.
     */
    @PostMapping("/select-organization")
    public ResponseEntity<SelectOrgResponse> selectOrganization(
            @RequestBody SelectOrganizationRequest request,
            HttpServletResponse httpResponse) {
        if (request == null || request.challengeToken() == null || request.organizationId() == null) {
            throw new IllegalArgumentException("challengeToken and organizationId are required");
        }

        try {
            AuthenticationService.LoginResult result = authenticationService.selectOrganization(
                    request.challengeToken(), request.organizationId(), currentTraceId());

            sessionCookieService.writeSessionCookie(
                    httpResponse, result.sessionId(), SESSION_COOKIE_TTL);

            return ResponseEntity.ok(new SelectOrgResponse(
                    result.sessionId(),
                    result.expiresAt().toString(),
                    result.tenantId(),
                    result.userId(),
                    currentTraceId(),
                    "success"));
        } catch (AuthChallengeService.AuthChallengeException ex) {
            throw new AuthSessionException(ex.getMessage());
        }
    }

    private static String currentTraceId() {
        String traceId = MDC.get("trace_id");
        return traceId == null ? "" : traceId;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * POST /api/auth/logout
     *
     * <p>T080 [US5] — Explicit logout: revokes the server-side session immediately
     * and clears the session cookie on the client.
     *
     * <p>Requires the session cookie or Authorization Bearer header.
     * Returns {@code {"revoked": true}} on success.
     *
     * Refs: FR-024
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request,
                                                  HttpServletResponse response) {
        UUID sessionId = resolveSessionIdOptional(request);
        if (sessionId != null) {
            TenantContext tenantContext = TenantContextStore.get();
            if (tenantContext != null) {
                try {
                    sessionManager.revokeSession(sessionId, tenantContext.tenantId());
                } catch (Exception ignored) {
                    // Best-effort revocation; cookie is cleared regardless.
                }
            }
        }
        sessionCookieService.clearSessionCookie(response);
        return ResponseEntity.ok(new LogoutResponse(true, currentTraceId()));
    }

    private UUID resolveSessionIdOptional(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                return UUID.fromString(authorization.substring(7).trim());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (sessionCookieService.cookieName().equals(cookie.getName())) {
                    try {
                        return UUID.fromString(cookie.getValue());
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    // ---- Request / Response DTOs ----

    public record LoginRequest(String email, String password) {}

    public record SelectOrganizationRequest(String challengeToken, UUID organizationId) {}

    public record OrganizationSummary(UUID organizationId, String displayName, String cnes) {}

    /**
     * Discriminated response.
     * {@code mode} = "single" | "multiple"
     */
    public record LoginMultiOrgResponse(
            String mode,
            // single-org fields (null when mode=multiple)
            UUID sessionId,
            String expiresAt,
            UUID organizationId,
            UUID userId,
            // multiple-orgs fields (null when mode=single)
            String challengeToken,
            List<OrganizationSummary> organizations,
            String traceId) {

        static LoginMultiOrgResponse single(UUID sessionId, String expiresAt,
                                             UUID organizationId, UUID userId,
                                             String traceId) {
            return new LoginMultiOrgResponse(
                    "single", sessionId, expiresAt, organizationId, userId,
                    null, null, traceId);
        }

        static LoginMultiOrgResponse multiple(String challengeToken,
                                               List<OrganizationSummary> organizations,
                                               String traceId) {
            return new LoginMultiOrgResponse(
                    "multiple", null, null, null, null,
                    challengeToken, organizations, traceId);
        }
    }

    public record SelectOrgResponse(
            UUID sessionId, String expiresAt, UUID tenantId,
            UUID userId, String traceId, String outcome) {}

    /** T080 [US5] Logout response. */
    public record LogoutResponse(boolean revoked, String traceId) {}
}
