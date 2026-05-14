package com.clinicadigital.gateway.api;

import com.clinicadigital.gateway.filters.AuthenticationFilter;
import com.clinicadigital.gateway.filters.TenantContextFilter;
import com.clinicadigital.iam.application.CreateProfile20UserResult;
import com.clinicadigital.iam.application.CreateProfile20UserService;
import com.clinicadigital.iam.application.SessionManager;
import com.clinicadigital.iam.application.UserContextService;
import com.clinicadigital.iam.domain.IamSession;
import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import com.clinicadigital.iam.domain.Practitioner;
import com.clinicadigital.iam.domain.PractitionerRepository;
import com.clinicadigital.iam.domain.PractitionerRole;
import com.clinicadigital.iam.domain.PractitionerRoleRepository;
import com.clinicadigital.iam.application.PiiCryptoService;
import com.clinicadigital.shared.api.TenantContextStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        private static final int SUPER_USER_PROFILE = 0;

    private final CreateProfile20UserService createProfile20UserService;
        private final SessionManager sessionManager;
    private final UserContextService userContextService;
    private final IamUserRepository iamUserRepository;
    private final PractitionerRepository practitionerRepository;
    private final PractitionerRoleRepository practitionerRoleRepository;
    private final PiiCryptoService piiCryptoService;

    public AdminUserController(
            CreateProfile20UserService createProfile20UserService,
                        SessionManager sessionManager,
            UserContextService userContextService,
            IamUserRepository iamUserRepository,
            PractitionerRepository practitionerRepository,
            PractitionerRoleRepository practitionerRoleRepository,
            PiiCryptoService piiCryptoService) {
        this.createProfile20UserService = createProfile20UserService;
                this.sessionManager = sessionManager;
        this.userContextService = userContextService;
        this.iamUserRepository = iamUserRepository;
        this.practitionerRepository = practitionerRepository;
        this.practitionerRoleRepository = practitionerRoleRepository;
        this.piiCryptoService = piiCryptoService;
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
        CallerAccess callerAccess;

        try {
            callerAccess = resolveCallerAccess(sessionId, tenantId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", ex.getMessage()));
        }

        List<TenantUserSummary> users = (callerAccess.globalAccess()
                ? iamUserRepository.findAll().stream()
                : iamUserRepository.findByTenantId(tenantId).stream())
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
    /**
     * Returns full detail (including FHIR practitioner data) for a single user.
     */
    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        Object rawSessionId = request.getAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR);
        if (rawSessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", "Session not found"));
        }
        UUID sessionId = (UUID) rawSessionId;
        UUID tenantId = TenantContextStore.get().tenantId();

        CallerAccess callerAccess;
        try {
            callerAccess = resolveCallerAccess(sessionId, tenantId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", ex.getMessage()));
        }

        Optional<IamUser> userOpt = callerAccess.globalAccess()
                ? iamUserRepository.findById(id)
                : iamUserRepository.findByIdAndTenantId(id, tenantId);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", "User not found: " + id));
        }

        IamUser user = userOpt.get();
        String fhirGender = null;
        String fhirBirthDate = null;
        String fhirTelecomJson = null;
        String fhirAddressJson = null;
        String fhirQualificationJson = null;
        String fhirCommunicationJson = null;

        String cpfMask = null;
        UUID locationId = null;
        String roleCode = null;
        UUID practitionerId = user.getPractitionerId();
        if (practitionerId != null) {
            Optional<Practitioner> practOpt = practitionerRepository.findById(practitionerId);
            if (practOpt.isPresent()) {
                Practitioner pract = practOpt.get();
                fhirGender = pract.getFhirGender();
                fhirBirthDate = pract.getFhirBirthDate() != null ? pract.getFhirBirthDate().toString() : null;
                fhirTelecomJson = pract.getFhirTelecomJson();
                fhirAddressJson = pract.getFhirAddressJson();
                fhirQualificationJson = pract.getFhirQualificationJson();
                fhirCommunicationJson = pract.getFhirCommunicationJson();
                if (pract.getCpfEncrypted() != null && pract.getEncryptionKeyVersion() != null) {
                    try {
                        cpfMask = piiCryptoService.decrypt(pract.getCpfEncrypted(), pract.getEncryptionKeyVersion());
                    } catch (Exception ignored) {
                        cpfMask = "***********";
                    }
                }
            }
            List<PractitionerRole> roles = practitionerRoleRepository.findActiveByPractitionerId(practitionerId);
            if (!roles.isEmpty()) {
                PractitionerRole pr = roles.get(0);
                locationId = pr.getLocationId();
                roleCode = pr.getRoleCode();
            }
        }

        return ResponseEntity.ok(new TenantUserDetailDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getProfile(),
                user.isActive(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                fhirGender,
                fhirBirthDate,
                fhirTelecomJson,
                fhirAddressJson,
                fhirQualificationJson,
                fhirCommunicationJson,
                cpfMask,
                locationId != null ? locationId.toString() : null,
                roleCode));
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
                        java.time.LocalDate birthDate = null;
                        if (body.practitioner().fhirBirthDate() != null && !body.practitioner().fhirBirthDate().isBlank()) {
                                try {
                                        birthDate = java.time.LocalDate.parse(body.practitioner().fhirBirthDate());
                                } catch (java.time.format.DateTimeParseException ignored) {
                                        // invalid date string: treat as null
                                }
                        }
                        CreateProfile20UserResult result = createProfile20UserService.create(
                                        tenantId,
                                        organizationId,
                                        body.locationId(),
                                        body.practitioner().displayName(),
                                        body.practitioner().email(),
                                        body.practitioner().cpf(),
                                        body.practitioner().password(),
                                        body.roleCode(),
                                        adminUserId,
                                        body.practitioner().fhirTelecomJson(),
                                        body.practitioner().fhirAddressJson(),
                                        body.practitioner().fhirGender(),
                                        birthDate,
                                        body.practitioner().fhirQualificationJson(),
                                        body.practitioner().fhirCommunicationJson());

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

    /**
     * Updates email, username and/or active status of an existing user.
     * Superuser (profile=0) can update any user; admin (profile=10) only within their tenant.
     */
    @Transactional
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable UUID id,
            HttpServletRequest request,
            @Valid @RequestBody UpdateUserRequest body) {

        Object rawSessionId = request.getAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR);
        if (rawSessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", "Session not found"));
        }
        UUID sessionId = (UUID) rawSessionId;
        UUID tenantId = TenantContextStore.get().tenantId();

        CallerAccess callerAccess;
        try {
            callerAccess = resolveCallerAccess(sessionId, tenantId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", ex.getMessage()));
        }

        // Fetch the target user, enforcing tenant scope for non-superuser callers
        Optional<IamUser> targetOpt = callerAccess.globalAccess()
                ? iamUserRepository.findById(id)
                : iamUserRepository.findByIdAndTenantId(id, tenantId);

        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", "User not found: " + id));
        }

        IamUser user = targetOpt.get();
        if (body.email() != null) user.setEmail(body.email());
        if (body.username() != null) user.setUsername(body.username());
        if (body.active() != null) user.setActive(body.active());
        iamUserRepository.save(user);

        UUID practitionerId = user.getPractitionerId();
        if (practitionerId != null) {
            practitionerRepository.findById(practitionerId).ifPresent(pract -> {
                if (body.cpf() != null && !body.cpf().isBlank() && body.cpf().matches("\\d{11}")) {
                    PiiCryptoService.EncryptedValue enc = piiCryptoService.encrypt(body.cpf());
                    pract.setCpfEncrypted(enc.cipherText());
                    pract.setEncryptionKeyVersion(enc.keyVersion());
                }
                if (body.fhirGender() != null)
                    pract.setFhirGender(body.fhirGender().isBlank() ? null : body.fhirGender());
                if (body.fhirBirthDate() != null) {
                    if (body.fhirBirthDate().isBlank()) {
                        pract.setFhirBirthDate(null);
                    } else {
                        try {
                            pract.setFhirBirthDate(java.time.LocalDate.parse(body.fhirBirthDate()));
                        } catch (java.time.format.DateTimeParseException ignored) {}
                    }
                }
                if (body.fhirTelecomJson() != null)
                    pract.setFhirTelecomJson(body.fhirTelecomJson().isBlank() ? null : body.fhirTelecomJson());
                if (body.fhirAddressJson() != null)
                    pract.setFhirAddressJson(body.fhirAddressJson().isBlank() ? null : body.fhirAddressJson());
                if (body.fhirQualificationJson() != null)
                    pract.setFhirQualificationJson(body.fhirQualificationJson().isBlank() ? null : body.fhirQualificationJson());
                if (body.fhirCommunicationJson() != null)
                    pract.setFhirCommunicationJson(body.fhirCommunicationJson().isBlank() ? null : body.fhirCommunicationJson());
                practitionerRepository.save(pract);
            });

            // Update PractitionerRole location/roleCode if provided
            if (body.locationId() != null || body.roleCode() != null) {
                List<PractitionerRole> roles = practitionerRoleRepository.findActiveByPractitionerId(practitionerId);
                if (!roles.isEmpty()) {
                    PractitionerRole pr = roles.get(0);
                    if (body.locationId() != null && !body.locationId().isBlank()) {
                        try { pr.setLocationId(java.util.UUID.fromString(body.locationId())); } catch (IllegalArgumentException ignored) {}
                    }
                    if (body.roleCode() != null && !body.roleCode().isBlank()) {
                        pr.setRoleCode(body.roleCode());
                    }
                    practitionerRoleRepository.save(pr);
                }
            }
        }

        return ResponseEntity.ok(new TenantUserSummary(
                user.getId(), user.getEmail(), user.getUsername(),
                user.getProfile(), user.isActive(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null));
    }

    /**
     * Deletes a user by id.
     * Superuser (profile=0) can delete any user; admin (profile=10) only within their tenant.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        Object rawSessionId = request.getAttribute(AuthenticationFilter.REQUEST_SESSION_ID_ATTR);
        if (rawSessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", "Session not found"));
        }
        UUID sessionId = (UUID) rawSessionId;
        UUID tenantId = TenantContextStore.get().tenantId();

        CallerAccess callerAccess;
        try {
            callerAccess = resolveCallerAccess(sessionId, tenantId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildOperationOutcome("security", ex.getMessage()));
        }

        Optional<IamUser> targetOpt = callerAccess.globalAccess()
                ? iamUserRepository.findById(id)
                : iamUserRepository.findByIdAndTenantId(id, tenantId);

        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", "User not found: " + id));
        }

        iamUserRepository.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record TenantUserDetailDto(
            UUID id,
            String email,
            String username,
            int profileType,
            boolean accountActive,
            String createdAt,
            String fhirGender,
            String fhirBirthDate,
            String fhirTelecomJson,
            String fhirAddressJson,
            String fhirQualificationJson,
            String fhirCommunicationJson,
            String cpf,
            String locationId,
            String roleCode
    ) {}

    public record TenantUserSummary(
            UUID id,
            String email,
            String username,
            int profileType,
            boolean accountActive,
            String createdAt
    ) {}

    public record UpdateUserRequest(
            String email,
            String username,
            Boolean active,
            String cpf,
            String locationId,
            String roleCode,
            String fhirGender,
            String fhirBirthDate,
            String fhirTelecomJson,
            String fhirAddressJson,
            String fhirQualificationJson,
            String fhirCommunicationJson
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
            @NotBlank String password,
            String fhirTelecomJson,
            String fhirAddressJson,
            @Pattern(regexp = "male|female|other|unknown",
                    message = "fhirGender must be one of: male, female, other, unknown")
            String fhirGender,
            String fhirBirthDate,
            String fhirQualificationJson,
            String fhirCommunicationJson
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

        private CallerAccess resolveCallerAccess(UUID sessionId, UUID tenantId) {
                IamSession session = sessionManager.findRequiredSession(sessionId);
                IamUser caller = iamUserRepository.findById(session.userId())
                                .orElseThrow(() -> new IllegalArgumentException("User not found for session: " + session.userId()));

                boolean systemTenantRequest = TenantContextFilter.SYSTEM_TENANT_ID.equals(tenantId);
                if (systemTenantRequest && caller.getProfile() == SUPER_USER_PROFILE) {
                        return new CallerAccess(caller.getId(), true);
                }

                UserContextService.UserContextResult ctx = userContextService.resolveContext(sessionId, tenantId);
                if (ctx.profileType() != REQUIRED_ADMIN_PROFILE) {
                        throw new IllegalArgumentException("Only admin users (profile=10) can access this endpoint");
                }
                return new CallerAccess(ctx.userId(), false);
        }

        private record CallerAccess(UUID userId, boolean globalAccess) {}

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
