package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.iam.domain.IamAuditEvent;
import com.clinicadigital.iam.domain.IamAuditEventRepository;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoint for reading the tenant audit trail.
 *
 * GET /api/admin/audit?limit=50 — returns recent audit events for the caller's tenant.
 *
 * Only callers with an active session may invoke this endpoint.
 * Results are scoped to the caller's tenant via RLS + tenantId filter.
 *
 * Refs: FR-016, FR-024
 */
@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final IamAuditEventRepository auditEventRepository;
    private final UserContextService userContextService;

    public AdminAuditController(
            IamAuditEventRepository auditEventRepository,
            UserContextService userContextService) {
        this.auditEventRepository = auditEventRepository;
        this.userContextService = userContextService;
    }

    @GetMapping
    public ResponseEntity<?> listAuditEvents(
            HttpServletRequest request,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {

        Object rawSessionId = request.getAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR);
        if (rawSessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", "Session not found"));
        }
        UUID sessionId = (UUID) rawSessionId;
        UUID tenantId = TenantContextStore.get().tenantId();

        try {
            userContextService.resolveContext(sessionId, tenantId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", ex.getMessage()));
        }

        int effectiveLimit = Math.min(Math.max(1, limit), MAX_LIMIT);
        List<AuditEventSummary> events = auditEventRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, effectiveLimit)
                .stream()
                .map(e -> new AuditEventSummary(
                        e.getId(),
                        e.getActorUserId(),
                        e.getEventType(),
                        e.getOutcome(),
                        e.getTraceId(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : null))
                .toList();

        return ResponseEntity.ok(events);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record AuditEventSummary(
            UUID id,
            UUID actorUserId,
            String eventType,
            String outcome,
            String traceId,
            String createdAt
    ) {}

    private static Map<String, Object> buildOperationOutcome(String code, String diagnostics) {
        return Map.of(
                "resourceType", "OperationOutcome",
                "issue", List.of(Map.of(
                        "severity", "error",
                        "code", code,
                        "diagnostics", diagnostics != null ? diagnostics : "")));
    }
}
