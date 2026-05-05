package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.iam.application.RbacGroupService;
import com.clinicadigital.iam.application.RbacGroupService.IamGroupResult;
import com.clinicadigital.iam.application.RbacGroupService.IamPermissionResult;
import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * T103 [US6] Admin endpoints for RBAC group and permission management.
 *
 * <ul>
 *   <li>POST /api/admin/groups — create a group (profile 10 required)</li>
 *   <li>GET  /api/admin/groups — list all groups for the tenant (profile 10 required)</li>
 *   <li>GET  /api/admin/permissions — list the global permission catalog (profile 10 required)</li>
 *   <li>POST /api/admin/groups/{groupId}/members — assign user to group (profile 10 required)</li>
 *   <li>POST /api/admin/groups/{groupId}/permissions — assign permission to group (profile 10 required)</li>
 *   <li>GET  /api/admin/groups/{groupId}/permissions — list permissions of a group (profile 10 required)</li>
 *   <li>GET  /api/admin/users/{userId}/permissions — list permissions of a user (profile 10 required)</li>
 * </ul>
 *
 * Refs: FR-006, plan.md RBAC matrix
 */
@RestController
@Validated
public class AdminGroupController {

    private static final int REQUIRED_ADMIN_PROFILE = 10;

    private final RbacGroupService rbacGroupService;
    private final UserContextService userContextService;

    public AdminGroupController(RbacGroupService rbacGroupService,
                                UserContextService userContextService) {
        this.rbacGroupService = rbacGroupService;
        this.userContextService = userContextService;
    }

    // ── Create group ──────────────────────────────────────────────────────────

    @PostMapping("/api/admin/groups")
    public ResponseEntity<?> createGroup(
            HttpServletRequest request,
            @Valid @RequestBody CreateGroupRequest body) {

        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        try {
            IamGroupResult result = rbacGroupService.createGroup(
                    auth.tenantId(), body.name(), body.description(), auth.adminUserId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "groupId", result.groupId(),
                            "tenantId", result.tenantId(),
                            "name", result.name(),
                            "description", result.description() != null ? result.description() : ""));

        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(buildOperationOutcome("forbidden", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildOperationOutcome("conflict", ex.getMessage()));
        }
    }

    @GetMapping("/api/admin/groups")
    public ResponseEntity<?> listGroups(HttpServletRequest request) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        List<IamGroupResult> groups = rbacGroupService.listGroups(auth.tenantId());
        return ResponseEntity.ok(groups);
    }

    // ── List global permissions ───────────────────────────────────────────────

    @GetMapping("/api/admin/permissions")
    public ResponseEntity<?> listPermissions(HttpServletRequest request) {
        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        List<IamPermissionResult> permissions = rbacGroupService.listPermissions();
        return ResponseEntity.ok(permissions);
    }

    // ── Assign user to group ──────────────────────────────────────────────────

    @PostMapping("/api/admin/groups/{groupId}/members")
    public ResponseEntity<?> assignUserToGroup(
            HttpServletRequest request,
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody AssignUserRequest body) {

        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        try {
            rbacGroupService.assignUserToGroup(
                    auth.tenantId(), groupId, body.userId(), auth.adminUserId());
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(buildOperationOutcome("forbidden", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        }
    }

    // ── Assign permission to group ────────────────────────────────────────────

    @PostMapping("/api/admin/groups/{groupId}/permissions")
    public ResponseEntity<?> assignPermissionToGroup(
            HttpServletRequest request,
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody AssignPermissionRequest body) {

        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        try {
            rbacGroupService.assignPermissionToGroup(
                    auth.tenantId(), groupId, body.permissionId(), auth.adminUserId());
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(buildOperationOutcome("forbidden", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        }
    }

    // ── List group permissions ────────────────────────────────────────────────

    @GetMapping("/api/admin/groups/{groupId}/permissions")
    public ResponseEntity<?> listGroupPermissions(
            HttpServletRequest request,
            @PathVariable("groupId") UUID groupId) {

        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        try {
            List<IamPermissionResult> permissions =
                    rbacGroupService.listGroupPermissions(auth.tenantId(), groupId);
            return ResponseEntity.ok(permissions);

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        }
    }

    // ── List user permissions ─────────────────────────────────────────────────

    @GetMapping("/api/admin/users/{userId}/permissions")
    public ResponseEntity<?> listUserPermissions(
            HttpServletRequest request,
            @PathVariable("userId") UUID userId) {

        var auth = resolveAdminContext(request);
        if (auth.error() != null) return auth.error();

        List<IamPermissionResult> permissions =
                rbacGroupService.listUserPermissions(auth.tenantId(), userId);
        return ResponseEntity.ok(permissions);
    }

    // ── Auth helper ───────────────────────────────────────────────────────────

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

        return AuthContext.ok(tenantId, ctx.userId());
    }

    private record AuthContext(
            UUID tenantId,
            UUID adminUserId,
            ResponseEntity<Object> error) {

        static AuthContext ok(UUID tenantId, UUID adminUserId) {
            return new AuthContext(tenantId, adminUserId, null);
        }

        static AuthContext unauthenticated() {
            return new AuthContext(null, null,
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(buildOperationOutcome("security", "Session required")));
        }

        static AuthContext forbidden() {
            return new AuthContext(null, null,
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(buildOperationOutcome("forbidden",
                                    "Only admin users (profile=10) can access this endpoint")));
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record CreateGroupRequest(
            String name,
            String description
    ) {}

    public record AssignUserRequest(
            @NotNull UUID userId
    ) {}

    public record AssignPermissionRequest(
            @NotNull UUID permissionId
    ) {}

    // ── Error helper ──────────────────────────────────────────────────────────

    private static Map<String, Object> buildOperationOutcome(String code, String diagnostics) {
        return Map.of(
                "resourceType", "OperationOutcome",
                "issue", List.of(Map.of(
                        "severity", "error",
                        "code", code,
                        "diagnostics", diagnostics != null ? diagnostics : "")));
    }
}
