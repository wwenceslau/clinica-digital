package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.iam.application.CreateProfile20UserResult;
import com.clinicadigital.iam.application.CreateProfile20UserService;
import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * T093 — Admin endpoint for creating profile-20 (regular practitioner) users.
 *
 * <ul>
 *   <li>POST /api/admin/users — create a new profile-20 user in the caller's tenant</li>
 * </ul>
 *
 * Only callers authenticated with profile=10 (admin) may invoke this endpoint.
 * Tenant context is resolved from the {@code X-Tenant-ID} header via {@link TenantContextStore}.
 * Session is resolved from {@code AuthenticationFilter.REQUEST_SESSION_ID_ATTR}.
 *
 * Refs: FR-006, FR-007, FR-009, FR-011, FR-016
 */
@RestController
@RequestMapping("/api/admin/users")
@Validated
public class AdminUserController {

    private static final int REQUIRED_ADMIN_PROFILE = 10;

    private final CreateProfile20UserService createProfile20UserService;
    private final UserContextService userContextService;
    private final IamUserRepository iamUserRepository;

    public AdminUserController(
            CreateProfile20UserService createProfile20UserService,
            UserContextService userContextService,
            IamUserRepository iamUserRepository) {
        this.createProfile20UserService = createProfile20UserService;
        this.userContextService = userContextService;
        this.iamUserRepository = iamUserRepository;
    }

    /**
     * Lists all iam_users within the caller's tenant.
     *
     * @param request HTTP request (used to read session ID from filter attribute)
     * @return 200 OK with list of {@link TenantUserSummary}
     */
    @GetMapping
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
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

        List<TenantUserSummary> users = iamUserRepository.findByTenantId(tenantId).stream()
                .map(u -> new TenantUserSummary(
                        u.getId(),
                        u.getEmail(),
                        u.getUsername(),
                        u.getProfile(),
                        u.isActive(),
                        u.getCreatedAt() != null ? u.getCreatedAt().toString() : null))
                .toList();
        return ResponseEntity.ok(users);
    }
     /*
     * @param request HTTP request (used to read session ID from filter attribute)
     * @param body    the validated request payload
     * @return 201 Created with {@link CreateTenantUserResponse},
     *         or 401/403/409/400 on error
     */
    @PostMapping
    public ResponseEntity<?> createUser(
            HttpServletRequest request,
            @Valid @RequestBody CreateTenantUserRequest body) {

        // ── 1. Resolve session and tenant from filter / context ───────────────
        Object rawSessionId = request.getAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR);
        if (rawSessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", "Session not found"));
        }
        UUID sessionId = (UUID) rawSessionId;
        UUID tenantId = TenantContextStore.get().tenantId();

        // ── 2. Resolve caller context and verify admin profile ────────────────
        UserContextService.UserContextResult ctx;
        try {
            ctx = userContextService.resolveContext(sessionId, tenantId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", ex.getMessage()));
        }

        if (ctx.profileType() != REQUIRED_ADMIN_PROFILE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(buildOperationOutcome("forbidden",
                            "Only admin users (profile=10) can create users"));
        }

        UUID adminUserId = ctx.userId();
        // organization.id == tenant.id (ck_organizations_tenant_is_self constraint)
        UUID organizationId = tenantId;

        // ── 3. Delegate to service ────────────────────────────────────────────
        try {
            CreateProfile20UserResult result = createProfile20UserService.create(
                    tenantId,
                    organizationId,
                    body.locationId(),
                    body.practitioner().displayName(),
                    body.practitioner().email(),
                    body.practitioner().cpf(),
                    body.practitioner().password(),
                    body.roleCode(),
                    adminUserId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CreateTenantUserResponse(
                            result.userId(),
                            result.practitionerId(),
                            result.practitionerRoleId(),
                            new PractitionerSummary(
                                    result.practitionerId(),
                                    body.practitioner().email(),
                                    20,
                                    body.practitioner().displayName(),
                                    true)));

        } catch (CreateProfile20UserService.EmailAlreadyTakenException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildOperationOutcome("conflict", ex.getMessage()));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildOperationOutcome("invalid", ex.getMessage()));
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record TenantUserSummary(
            UUID id,
            String email,
            String username,
            int profileType,
            boolean accountActive,
            String createdAt
    ) {}

    public record CreateTenantUserRequest(
            @Valid PractitionerCreateInput practitioner,
            @NotNull UUID locationId,
            @NotBlank String roleCode
    ) {}

    public record PractitionerCreateInput(
            @NotBlank String displayName,
            @NotBlank String email,
            @NotBlank @Pattern(regexp = "\\d{11}",
                    message = "cpf must be exactly 11 numeric digits") String cpf,
            @NotBlank String password
    ) {}

    public record CreateTenantUserResponse(
            UUID userId,
            UUID practitionerId,
            UUID practitionerRoleId,
            PractitionerSummary practitioner
    ) {}

    public record PractitionerSummary(
            UUID id,
            String email,
            int profileType,
            String displayName,
            boolean accountActive
    ) {}

    // ── Error helpers ─────────────────────────────────────────────────────────

    private static Map<String, Object> buildOperationOutcome(String code, String diagnostics) {
        return Map.of(
                "resourceType", "OperationOutcome",
                "issue", List.of(Map.of(
                        "severity", "error",
                        "code", code,
                        "diagnostics", diagnostics != null ? diagnostics : "")));
    }
}
