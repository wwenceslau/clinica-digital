package com.clinicadigital.gateway.api;

import com.clinicadigital.iam.application.AdminEmailAlreadyExistsException;
import com.clinicadigital.iam.application.CreateTenantAdminResult;
import com.clinicadigital.iam.application.CreateTenantAdminService;
import com.clinicadigital.iam.application.TenantAlreadyExistsException;
import com.clinicadigital.tenant.application.TenantService;
import com.clinicadigital.tenant.domain.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * T034 [US2] REST controller for the admin tenant-provisioning endpoint.
 *
 * <pre>POST /api/admin/tenants</pre>
 *
 * Creates a tenant, its organization and the first admin practitioner in one
 * atomic transaction. Returns 201 on success, 409 Conflict when the CNES /
 * organization name / admin email already exists, and 400 for invalid input.
 *
 * <p>This endpoint is intended for platform operations (not available to
 * regular authenticated tenant users). The corresponding CLI command is
 * {@code create-tenant-admin}.
 *
 * Refs: FR-003, FR-009, FR-022, api-openapi.yaml
 */
@RestController
@RequestMapping("/api/admin/tenants")
@Validated
public class AdminTenantController {

    private final CreateTenantAdminService createTenantAdminService;
    private final TenantService tenantService;
        private final TenantAdminProfileService tenantAdminProfileService;

    public AdminTenantController(CreateTenantAdminService createTenantAdminService,
                                                                  TenantService tenantService,
                                                                  TenantAdminProfileService tenantAdminProfileService) {
        this.createTenantAdminService = createTenantAdminService;
        this.tenantService = tenantService;
                this.tenantAdminProfileService = tenantAdminProfileService;
    }

    /**
     * T136 [US2] List all tenants (authenticated, admin only).
     *
     * <pre>GET /api/admin/tenants</pre>
     *
     * Refs: FR-003
     */
    @GetMapping
    public ResponseEntity<List<TenantSummaryResponse>> listTenants() {
        List<TenantSummaryResponse> tenants = tenantService.listTenants().stream()
                .map(t -> {
                    TenantAdminProfileService.TenantAdminSummary admin = tenantAdminProfileService
                            .findTenantAdmin(t.getId())
                            .orElse(null);
                    return new TenantSummaryResponse(
                            t.getId(),
                            t.getSlug(),
                            t.getLegalName(),
                            t.getStatus(),
                            t.getPlanTier(),
                            admin != null ? admin.displayName() : null,
                            admin != null ? admin.email() : null,
                            admin != null ? admin.cpf() : null);
                })
                .toList();
        return ResponseEntity.ok(tenants);
    }

    @PostMapping
    public ResponseEntity<?> createTenantAdmin(
            @Valid @RequestBody CreateTenantAdminRequest request) {

        try {
            CreateTenantAdminResult result = createTenantAdminService.create(
                    request.organization().displayName(),
                    request.organization().cnes(),
                    request.adminPractitioner().displayName(),
                    request.adminPractitioner().email(),
                    request.adminPractitioner().cpf(),
                    request.adminPractitioner().password());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CreateTenantAdminResponse(
                            result.tenantId(),
                            result.adminPractitionerId(),
                            new OrganizationSummary(
                                    request.organization().displayName(),
                                    request.organization().cnes(),
                                    true),
                            new PractitionerSummary(
                                    result.adminPractitionerId(),
                                    request.adminPractitioner().email(),
                                    10,
                                    request.adminPractitioner().displayName(),
                                    true)));

        } catch (TenantAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildOperationOutcome("conflict", ex.getMessage()));

        } catch (AdminEmailAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildOperationOutcome("conflict", ex.getMessage()));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildOperationOutcome("invalid", ex.getMessage()));
        }
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<?> updateTenant(
            @PathVariable("tenantId") UUID tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {
        try {
            String planTier = request.planTier();
            if (planTier == null || planTier.isBlank()) {
                planTier = tenantService.getTenant(tenantId).getPlanTier();
            }

            Tenant updated = tenantService.updateTenant(
                    tenantId,
                    request.organization().cnes(),
                    request.organization().displayName(),
                    planTier);

            TenantAdminProfileService.TenantAdminSummary admin = tenantAdminProfileService.updateTenantAdmin(
                    tenantId,
                    request.organization().displayName(),
                    request.organization().cnes(),
                    request.adminPractitioner().displayName(),
                    request.adminPractitioner().email(),
                    request.adminPractitioner().cpf(),
                    request.adminPractitioner().password());

            return ResponseEntity.ok(new TenantSummaryResponse(
                    updated.getId(),
                    updated.getSlug(),
                    updated.getLegalName(),
                    updated.getStatus(),
                    updated.getPlanTier(),
                    admin.displayName(),
                    admin.email(),
                    admin.cpf()));
        } catch (TenantAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildOperationOutcome("conflict", ex.getMessage()));
        } catch (AdminEmailAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildOperationOutcome("conflict", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildOperationOutcome("invalid", ex.getMessage()));
        }
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<?> deleteTenant(@PathVariable("tenantId") UUID tenantId) {
        try {
            tenantService.deleteTenant(tenantId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildOperationOutcome("not-found", ex.getMessage()));
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record CreateTenantAdminRequest(
            @Valid OrganizationCreateInput organization,
            @Valid PractitionerCreateInput adminPractitioner
    ) {}

    public record OrganizationCreateInput(
            @NotBlank String displayName,
            @NotBlank @Pattern(regexp = "\\d{7}",
                    message = "cnes must be exactly 7 numeric digits") String cnes
    ) {}

    public record PractitionerCreateInput(
            @NotBlank String displayName,
            @NotBlank String email,
            @NotBlank @Pattern(regexp = "\\d{11}",
                    message = "cpf must be exactly 11 numeric digits") String cpf,
            @NotBlank String password
    ) {}

    public record TenantSummaryResponse(
            UUID id,
            String slug,
            String legalName,
            String status,
            String planTier,
            String adminDisplayName,
            String adminEmail,
            String adminCpf
    ) {}

    public record UpdateTenantRequest(
            @Valid OrganizationCreateInput organization,
            @Valid PractitionerCreateInput adminPractitioner,
            String planTier
    ) {}

    public record CreateTenantAdminResponse(
            UUID tenantId,
            UUID adminPractitionerId,
            OrganizationSummary organization,
            PractitionerSummary adminPractitioner
    ) {}

    public record OrganizationSummary(
            String displayName,
            String cnes,
            boolean accountActive
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
