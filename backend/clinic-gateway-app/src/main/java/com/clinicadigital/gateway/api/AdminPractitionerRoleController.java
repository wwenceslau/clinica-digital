package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.iam.application.AdminPractitionerRoleService;
import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Administrative endpoints for practitioner roles CRUD inside a tenant context.
 *
 * Refs: FR-006, FR-019, FR-020
 */
@RestController
@RequestMapping("/api/admin/practitioner-roles")
@Validated
public class AdminPractitionerRoleController {

    private static final int REQUIRED_ADMIN_PROFILE = 10;

    private final AdminPractitionerRoleService adminPractitionerRoleService;
    private final UserContextService userContextService;

    public AdminPractitionerRoleController(AdminPractitionerRoleService adminPractitionerRoleService,
                                           UserContextService userContextService) {
        this.adminPractitionerRoleService = adminPractitionerRoleService;
        this.userContextService = userContextService;
    }

    @GetMapping
    public ResponseEntity<?> listRoles(HttpServletRequest request) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) {
            return auth.error();
        }

        return ResponseEntity.ok(adminPractitionerRoleService.listByTenant(auth.tenantId()));
    }

    @PostMapping
    public ResponseEntity<?> createRole(HttpServletRequest request,
                                        @Valid @RequestBody CreatePractitionerRoleRequest body) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) {
            return auth.error();
        }

        try {
            var result = adminPractitionerRoleService.create(
                    auth.tenantId(),
                    body.organizationId(),
                    body.locationId(),
                    body.practitionerId(),
                    body.roleCode(),
                    body.primaryRole() != null && body.primaryRole(),
                    body.periodStart(),
                    body.periodEnd(),
                    body.fhirCodeJson(),
                    body.fhirSpecialtyJson(),
                    body.fhirTelecomJson(),
                    body.fhirAvailableTimeJson());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildOperationOutcome("invalid", ex.getMessage()));
        }
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<?> updateRole(HttpServletRequest request,
                                        @PathVariable UUID roleId,
                                        @Valid @RequestBody UpdatePractitionerRoleRequest body) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) {
            return auth.error();
        }

        try {
            var result = adminPractitionerRoleService.update(
                    auth.tenantId(),
                    roleId,
                    body.roleCode(),
                    body.active(),
                    body.primaryRole(),
                    body.periodStart(),
                    body.periodEnd(),
                    body.fhirCodeJson(),
                    body.fhirSpecialtyJson(),
                    body.fhirTelecomJson(),
                    body.fhirAvailableTimeJson());
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        }
    }

    @PostMapping("/{roleId}/deactivate")
    public ResponseEntity<?> deactivateRole(HttpServletRequest request,
                                            @PathVariable UUID roleId) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) {
            return auth.error();
        }

        try {
            var result = adminPractitionerRoleService.deactivate(auth.tenantId(), roleId);
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        }
    }

    private AuthContext resolveAdminContext(HttpServletRequest request) {
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

        if (ctx.profileType() != REQUIRED_ADMIN_PROFILE) {
            return AuthContext.forbidden();
        }

        return AuthContext.ok(tenantId);
    }

    private record AuthContext(
            UUID tenantId,
            ResponseEntity<Object> error) {

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
                            .body(buildOperationOutcome("forbidden",
                                    "Only admin users (profile=10) can access this endpoint")));
        }
    }

    public record CreatePractitionerRoleRequest(
            @NotNull UUID organizationId,
            @NotNull UUID locationId,
            @NotNull UUID practitionerId,
            @NotBlank String roleCode,
            Boolean primaryRole,
            Instant periodStart,
            Instant periodEnd,
            String fhirCodeJson,
            String fhirSpecialtyJson,
            String fhirTelecomJson,
            String fhirAvailableTimeJson) {
    }

    public record UpdatePractitionerRoleRequest(
            String roleCode,
            Boolean active,
            Boolean primaryRole,
            Instant periodStart,
            Instant periodEnd,
            String fhirCodeJson,
            String fhirSpecialtyJson,
            String fhirTelecomJson,
            String fhirAvailableTimeJson) {
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
