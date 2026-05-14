package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.iam.domain.IamSession;
import com.clinicadigital.iam.domain.IamSessionRepository;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoint for tenant-scoped session listing and revocation.
 */
@RestController
@Validated
@RequestMapping("/api/admin/sessions")
public class AdminSessionController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final IamSessionRepository sessionRepository;
    private final UserContextService userContextService;

    public AdminSessionController(IamSessionRepository sessionRepository, UserContextService userContextService) {
        this.sessionRepository = sessionRepository;
        this.userContextService = userContextService;
    }

    @GetMapping
    public ResponseEntity<?> listSessions(
            HttpServletRequest request,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {

        var auth = resolveAdminOrSuperContext(request);
        if (auth.error() != null) {
            return auth.error();
        }

        int effectiveLimit = Math.min(Math.max(1, limit), MAX_LIMIT);
        List<SessionSummary> sessions = sessionRepository
                .findByTenantIdOrderByIssuedAtDesc(auth.tenantId(), effectiveLimit)
                .stream()
                .map(this::toSummary)
                .toList();

        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/{sessionId}/revoke")
    public ResponseEntity<?> revokeSession(
            HttpServletRequest request,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody RevokeSessionRequest body) {

        var auth = resolveAdminOrSuperContext(request);
        if (auth.error() != null) {
            return auth.error();
        }

        String digest = sha256Hex(sessionId.toString());
        var session = sessionRepository.findByOpaqueTokenDigest(digest)
                .filter(s -> auth.tenantId().equals(s.tenantId()))
                .orElse(null);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", "Session not found for tenant"));
        }

        String reason = (body.revocationReason() == null || body.revocationReason().isBlank())
                ? "admin_revoke"
                : body.revocationReason().trim();

        sessionRepository.revokeByOpaqueTokenDigest(digest, auth.tenantId(), reason);
        return ResponseEntity.noContent().build();
    }

    private SessionSummary toSummary(IamSession session) {
        return new SessionSummary(
                session.id(),
                session.tenantId(),
                session.userId(),
                session.organizationId(),
                session.issuedAt() != null ? session.issuedAt().toString() : null,
                session.expiresAt() != null ? session.expiresAt().toString() : null,
                session.revokedAt() != null ? session.revokedAt().toString() : null,
                session.traceId(),
                session.clientIp(),
                session.userAgent(),
                session.revocationReason(),
                session.activeFlag());
    }

    private AuthContext resolveAdminOrSuperContext(HttpServletRequest request) {
        Object rawSessionId = request.getAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR);
        if (rawSessionId == null) {
            return AuthContext.unauthenticated();
        }

        UUID sessionId = (UUID) rawSessionId;
        UUID tenantId = TenantContextStore.get().tenantId();

        UserContextService.UserContextResult ctx;
        try {
            ctx = userContextService.resolveContext(sessionId, tenantId);
        } catch (IllegalArgumentException ex) {
            return AuthContext.unauthenticated();
        }

        if (ctx.profileType() != 0 && ctx.profileType() != 10) {
            return AuthContext.forbidden();
        }

        return AuthContext.ok(tenantId);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record AuthContext(UUID tenantId, ResponseEntity<Object> error) {
        static AuthContext ok(UUID tenantId) {
            return new AuthContext(tenantId, null);
        }

        static AuthContext unauthenticated() {
            return new AuthContext(null,
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(buildOperationOutcome("security", "Session required")));
        }

        static AuthContext forbidden() {
            return new AuthContext(null,
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(buildOperationOutcome("forbidden", "Only admin users can access this endpoint")));
        }
    }

    public record SessionSummary(
            UUID id,
            UUID tenantId,
            UUID userId,
            UUID organizationId,
            String issuedAt,
            String expiresAt,
            String revokedAt,
            String traceId,
            String clientIp,
            String userAgent,
            String revocationReason,
            boolean active
    ) {
    }

    public record RevokeSessionRequest(
            @Size(max = 64) String revocationReason
    ) {
    }

    private static Map<String, Object> buildOperationOutcome(String code, String diagnostics) {
        return Map.of(
                "resourceType", "OperationOutcome",
                "issue", List.of(Map.of(
                        "severity", "error",
                        "code", code,
                        "diagnostics", diagnostics != null ? diagnostics : "")));
    }
}
