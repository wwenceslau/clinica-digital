package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.exception.AuthSessionException;
import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.iam.application.UserContextService.NoActivePractitionerRoleException;
import com.clinicadigital.iam.application.UserContextService.UserContextResult;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the user context endpoints.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@code GET  /api/users/me/context} — resolve and return the authenticated user context.</li>
 *   <li>{@code POST /api/users/me/active-location} — select an active location for the session.</li>
 * </ul>
 *
 * Refs: FR-008, FR-018, FR-019, US11
 */
@RestController
@RequestMapping("/api/users/me")
public class UserContextController {

    private final UserContextService userContextService;

    public UserContextController(UserContextService userContextService) {
        this.userContextService = userContextService;
    }

    /**
     * T086: returns the resolved user context for the current authenticated session.
     *
     * <p>The session ID is populated by {@link AuthenticationFilter} as a request attribute.
     * The tenant ID comes from {@link TenantContextStore} (set by TenantContextFilter).
     */
    @GetMapping("/context")
    public ResponseEntity<UserContextResponse> getContext(HttpServletRequest request) {
        UUID sessionId = resolveSessionId(request);
        UUID tenantId = resolveTenantId();

        UserContextResult result = userContextService.resolveContext(sessionId, tenantId);
        return ResponseEntity.ok(UserContextResponse.from(result));
    }

    /**
     * T087: selects the active location for the session.
     *
     * <p>The practitioner must have an active {@link com.clinicadigital.iam.domain.PractitionerRole}
     * linking them to the requested location within the tenant. Otherwise, 403 is returned.
     */
    @PostMapping("/active-location")
    public ResponseEntity<?> setActiveLocation(
            @Valid @RequestBody ActiveLocationRequest body,
            HttpServletRequest request) {
        UUID sessionId = resolveSessionId(request);
        UUID tenantId = resolveTenantId();

        try {
            UserContextResult result = userContextService.setActiveLocation(sessionId, tenantId, body.locationId());
            return ResponseEntity.ok(UserContextResponse.from(result));
        } catch (NoActivePractitionerRoleException e) {
            return ResponseEntity.status(403).body(operationOutcome("forbidden", e.getMessage()));
        }
    }

    // ---- Private helpers ----

    private UUID resolveSessionId(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR);
        if (attr == null) {
            throw new AuthSessionException("request.session_id must not be null");
        }
        return (UUID) attr;
    }

    private UUID resolveTenantId() {
        TenantContext context = TenantContextStore.get();
        if (context == null || context.tenantId() == null) {
            throw new AuthSessionException("X-Tenant-ID header is required");
        }
        return context.tenantId();
    }

    private Map<String, Object> operationOutcome(String code, String diagnostics) {
        String safeDiagnostics = diagnostics == null || diagnostics.isBlank() ? "unexpected error" : diagnostics;
        return Map.of(
                "resourceType", "OperationOutcome",
                "issue", List.of(Map.of(
                        "severity", "error",
                        "code", code,
                        "details", Map.of("text", safeDiagnostics),
                        "diagnostics", safeDiagnostics
                ))
        );
    }

    // ---- Request / Response DTOs ----

    /**
     * Request body for {@code POST /api/users/me/active-location}.
     */
    public record ActiveLocationRequest(@NotNull UUID locationId) {}

    /**
     * Response body for both context endpoints.
     * Refs: contracts/api-openapi.yaml — UserContextResponse schema
     */
    public record UserContextResponse(
            UUID tenantId,
            UUID organizationId,
            String organizationName,
            UUID locationId,
            String locationName,
            UUID practitionerId,
            String practitionerName,
            int profileType,
            UUID practitionerRoleId,
            String roleCode
    ) {
        static UserContextResponse from(UserContextResult result) {
            return new UserContextResponse(
                    result.tenantId(),
                    result.organizationId(),
                    result.organizationName(),
                    result.locationId(),
                    result.locationName(),
                    result.practitionerId(),
                    result.practitionerName(),
                    result.profileType(),
                    result.practitionerRoleId(),
                    result.roleCode()
            );
        }
    }
}
