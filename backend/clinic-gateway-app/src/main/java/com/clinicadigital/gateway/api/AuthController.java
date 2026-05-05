package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.exception.AuthSessionException;
import com.clinicadigital.gateway.exception.InvalidTenantContextException;
import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.gateway.security.LoginLockoutService;
import com.clinicadigital.gateway.security.SanitizationValidationGate;
import com.clinicadigital.gateway.security.SessionCookieService;
import com.clinicadigital.iam.application.AuthenticationService;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.time.Duration;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private static final Duration SESSION_COOKIE_TTL = Duration.ofMinutes(30);

    private final AuthenticationService authenticationService;
    private final SanitizationValidationGate sanitizationValidationGate;
    private final LoginLockoutService loginLockoutService;
    private final SessionCookieService sessionCookieService;

    public AuthController(AuthenticationService authenticationService,
                          SanitizationValidationGate sanitizationValidationGate,
                          LoginLockoutService loginLockoutService,
                          SessionCookieService sessionCookieService) {
        this.authenticationService = authenticationService;
        this.sanitizationValidationGate = sanitizationValidationGate;
        this.loginLockoutService = loginLockoutService;
        this.sessionCookieService = sessionCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        TenantContext context = requiredTenantContext();
        if (request == null) {
            throw new AuthSessionException("email and password are required");
        }

        String email = sanitizationValidationGate.requireEmail(request.email(), "email");
        String password = sanitizationValidationGate.requirePassword(request.password(), "password");

        UUID effectiveTenantId = request.tenantId() == null ? context.tenantId() : request.tenantId();
        if (request.tenantId() != null && !context.tenantId().equals(request.tenantId())) {
            throw new InvalidTenantContextException("tenant context invalid: body tenant does not match context tenant");
        }

        String lockoutKey = effectiveTenantId + ":" + email;
        loginLockoutService.assertNotLocked(lockoutKey);

        try {
            AuthenticationService.LoginResult result = authenticationService.login(
                    effectiveTenantId,
                email,
                password,
                    currentTraceId(),
                    clientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"));

            loginLockoutService.registerSuccess(lockoutKey);
            sessionCookieService.writeSessionCookie(httpResponse, result.sessionId(), SESSION_COOKIE_TTL);

            return ResponseEntity.ok(new LoginResponse(
                    result.sessionId(),
                    result.expiresAt().toString(),
                    result.tenantId(),
                    result.userId(),
                    result.traceId(),
                    "auth.login",
                    "success"));
        } catch (IllegalArgumentException ex) {
            loginLockoutService.registerFailure(lockoutKey);
            throw new AuthSessionException(ex.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request,
                                                 HttpServletResponse response) {
        TenantContext context = requiredTenantContext();
        UUID sessionId = requiredRequestSessionId(request);

        authenticationService.logout(sessionId, context.tenantId(), null, currentTraceId());
        sessionCookieService.clearSessionCookie(response);
        return ResponseEntity.ok(new LogoutResponse(
                sessionId,
                true,
                currentTraceId(),
                "auth.logout",
                "success"));
    }

    @GetMapping("/whoami")
    public ResponseEntity<WhoAmIResponse> whoami(HttpServletRequest request) {
        TenantContext context = requiredTenantContext();
        UUID sessionId = requiredRequestSessionId(request);

        AuthenticationService.WhoAmIResult result = authenticationService.whoami(
                sessionId,
                context.tenantId(),
                currentTraceId());

        return ResponseEntity.ok(new WhoAmIResponse(
                result.userId(),
                result.email(),
                result.tenantId(),
                result.roles(),
                result.traceId(),
                "auth.whoami",
                "success"));
    }

    private static TenantContext requiredTenantContext() {
        TenantContext context = TenantContextStore.get();
        if (context == null) {
            throw new InvalidTenantContextException("tenant context missing for auth operation");
        }
        return context;
    }

    private static UUID requiredRequestSessionId(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR);
        if (!(attr instanceof UUID sessionId)) {
            throw new AuthSessionException("request.session_id must not be null");
        }
        return sessionId;
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

    public record LoginRequest(
            UUID tenantId,
            String email,
            String password
    ) {
    }

    public record LoginResponse(
            UUID sessionId,
            String expiresAt,
            UUID tenantId,
            UUID userId,
            String traceId,
            String operation,
            String outcome
    ) {
    }

    public record LogoutResponse(
            UUID sessionId,
            boolean revoked,
            String traceId,
            String operation,
            String outcome
    ) {
    }

    public record WhoAmIResponse(
            UUID userId,
            String email,
            UUID tenantId,
            List<String> roles,
            String traceId,
            String operation,
            String outcome
    ) {
    }
}