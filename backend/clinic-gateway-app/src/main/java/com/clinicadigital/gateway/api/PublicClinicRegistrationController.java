package com.clinicadigital.gateway.api;

import com.clinicadigital.iam.application.AdminEmailAlreadyExistsException;
import com.clinicadigital.iam.application.CreateTenantAdminResult;
import com.clinicadigital.iam.application.CreateTenantAdminService;
import com.clinicadigital.iam.application.TenantAlreadyExistsException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * T044 [US3] Public REST endpoint for clinic self-registration.
 *
 * <pre>POST /api/public/clinic-registration</pre>
 *
 * Creates a new tenant, its organization, and the first admin practitioner (profile 10)
 * atomically. This endpoint is publicly accessible — no authentication or
 * {@code X-Tenant-ID} header is required. The {@link
 * com.clinicadigital.gateway.filters.TenantContextFilter} automatically binds the
 * system tenant context for all {@code /api/public/**} paths.
 *
 * <p>Returns:
 * <ul>
 *   <li>201 Created — clinic and admin created successfully.</li>
 *   <li>409 Conflict — CNES or admin email already registered (FHIR OperationOutcome).</li>
 *   <li>400 Bad Request — request validation failure (FHIR OperationOutcome).</li>
 * </ul>
 *
 * <p>T045: Reuses {@link CreateTenantAdminService#create(String, String, String, String, String, String)}
 * from {@code clinic-iam-core} without modification. Public-specific input
 * constraints (CNES 7 digits, CPF 11 digits) are enforced by Bean Validation
 * annotations on the request DTO.
 *
 * Refs: FR-003, FR-009, FR-013, FR-022, api-openapi.yaml
 */
@RestController
@RequestMapping("/api/public/clinic-registration")
@Validated
public class PublicClinicRegistrationController {

    private final CreateTenantAdminService createTenantAdminService;

    public PublicClinicRegistrationController(CreateTenantAdminService createTenantAdminService) {
        this.createTenantAdminService = createTenantAdminService;
    }

    @PostMapping
    public ResponseEntity<?> registerClinic(
            @Valid @RequestBody ClinicRegistrationRequest request) {

        try {
            CreateTenantAdminResult result = createTenantAdminService.create(
                    request.organization().displayName(),
                    request.organization().cnes(),
                    request.adminPractitioner().displayName(),
                    request.adminPractitioner().email(),
                    request.adminPractitioner().cpf(),
                    request.adminPractitioner().password());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ClinicRegistrationResponse(
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

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record ClinicRegistrationRequest(
            @Valid OrganizationInput organization,
            @Valid PractitionerInput adminPractitioner
    ) {}

    public record OrganizationInput(
            @NotBlank String displayName,
            @NotBlank @Pattern(regexp = "\\d{7}",
                    message = "cnes must be exactly 7 numeric digits") String cnes
    ) {}

    public record PractitionerInput(
            @NotBlank String displayName,
            @NotBlank String email,
            @NotBlank @Pattern(regexp = "\\d{11}",
                    message = "cpf must be exactly 11 numeric digits") String cpf,
            @NotBlank String password
    ) {}

    public record ClinicRegistrationResponse(
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
