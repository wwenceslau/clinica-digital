package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.iam.application.AdminLocationService;
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

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Administrative endpoints for locations CRUD inside a tenant context.
 *
 * Refs: FR-018, FR-019, FR-020
 */
@RestController
@RequestMapping("/api/admin/locations")
@Validated
public class AdminLocationController {

    private static final int REQUIRED_ADMIN_PROFILE = 10;

    private final AdminLocationService adminLocationService;
    private final UserContextService userContextService;

    public AdminLocationController(AdminLocationService adminLocationService,
                                   UserContextService userContextService) {
        this.adminLocationService = adminLocationService;
        this.userContextService = userContextService;
    }

    @GetMapping
    public ResponseEntity<?> listLocations(HttpServletRequest request) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        List<AdminLocationService.LocationResult> locations =
                adminLocationService.listByTenant(auth.tenantId());
        return ResponseEntity.ok(locations);
    }

    @PostMapping
    public ResponseEntity<?> createLocation(HttpServletRequest request,
                                            @Valid @RequestBody CreateLocationRequest body) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        try {
            AdminLocationService.LocationResult result = adminLocationService.create(
                    auth.tenantId(),
                    body.organizationId(),
                    body.displayName(),
                    body.fhirName(),
                    body.fhirStatus(),
                    body.fhirMode(),
                    body.fhirTelecomJson(),
                    body.fhirAddressJson());

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildOperationOutcome("invalid", ex.getMessage()));
        }
    }

    @PutMapping("/{locationId}")
    public ResponseEntity<?> updateLocation(HttpServletRequest request,
                                            @PathVariable UUID locationId,
                                            @Valid @RequestBody UpdateLocationRequest body) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        try {
            AdminLocationService.LocationResult result = adminLocationService.update(
                    auth.tenantId(),
                    locationId,
                    body.displayName(),
                    body.fhirName(),
                    body.fhirStatus(),
                    body.fhirMode(),
                    body.accountActive(),
                    body.fhirTelecomJson(),
                    body.fhirAddressJson());
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        }
    }

    @PostMapping("/{locationId}/deactivate")
    public ResponseEntity<?> deactivateLocation(HttpServletRequest request,
                                                @PathVariable UUID locationId) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        try {
            AdminLocationService.LocationResult result =
                    adminLocationService.deactivate(auth.tenantId(), locationId);
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

    public record CreateLocationRequest(
            @NotNull UUID organizationId,
            @NotBlank String displayName,
            String fhirName,
            String fhirStatus,
            String fhirMode,
            String fhirTelecomJson,
            String fhirAddressJson) {
    }

    public record UpdateLocationRequest(
            String displayName,
            String fhirName,
            String fhirStatus,
            String fhirMode,
            Boolean accountActive,
            String fhirTelecomJson,
            String fhirAddressJson) {
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
