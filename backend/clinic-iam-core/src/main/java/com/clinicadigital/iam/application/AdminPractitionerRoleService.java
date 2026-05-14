package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.LocationRepository;
import com.clinicadigital.iam.domain.Organization;
import com.clinicadigital.iam.domain.OrganizationRepository;
import com.clinicadigital.iam.domain.PractitionerRepository;
import com.clinicadigital.iam.domain.PractitionerRole;
import com.clinicadigital.iam.domain.PractitionerRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Administrative CRUD for practitioner roles in a tenant.
 *
 * Refs: FR-006, FR-019, FR-020
 */
@Service
@Transactional
public class AdminPractitionerRoleService {

    private static final String DEFAULT_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";

    private final PractitionerRoleRepository practitionerRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final LocationRepository locationRepository;
    private final PractitionerRepository practitionerRepository;

    public AdminPractitionerRoleService(PractitionerRoleRepository practitionerRoleRepository,
                                        OrganizationRepository organizationRepository,
                                        LocationRepository locationRepository,
                                        PractitionerRepository practitionerRepository) {
        this.practitionerRoleRepository = practitionerRoleRepository;
        this.organizationRepository = organizationRepository;
        this.locationRepository = locationRepository;
        this.practitionerRepository = practitionerRepository;
    }

    @Transactional(readOnly = true)
    public List<PractitionerRoleResult> listByTenant(UUID tenantId) {
        return practitionerRoleRepository.findByTenantId(tenantId).stream()
                .map(this::toResult)
                .toList();
    }

    public PractitionerRoleResult create(UUID tenantId,
                                         UUID organizationId,
                                         UUID locationId,
                                         UUID practitionerId,
                                         String roleCode,
                                         boolean primaryRole,
                                         String fhirCodeJson,
                                         String fhirSpecialtyJson,
                                         String fhirTelecomJson,
                                         String fhirAvailableTimeJson) {
        return create(
                tenantId,
                organizationId,
                locationId,
                practitionerId,
                roleCode,
                primaryRole,
                null,
                null,
                fhirCodeJson,
                fhirSpecialtyJson,
                fhirTelecomJson,
                fhirAvailableTimeJson);
    }

    public PractitionerRoleResult create(UUID tenantId,
                                         UUID organizationId,
                                         UUID locationId,
                                         UUID practitionerId,
                                         String roleCode,
                                         boolean primaryRole,
                                         Instant periodStart,
                                         Instant periodEnd,
                                         String fhirCodeJson,
                                         String fhirSpecialtyJson,
                                         String fhirTelecomJson,
                                         String fhirAvailableTimeJson) {
        validateOrganizationInTenant(organizationId, tenantId);
        locationRepository.findByIdAndTenantId(locationId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Location not found: " + locationId));
        practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new NoSuchElementException("Practitioner not found: " + practitionerId));

        UUID id = UUID.randomUUID();
        PractitionerRole role = new PractitionerRole(
                id,
                tenantId,
                organizationId,
                locationId,
                practitionerId,
                "role-" + id,
                DEFAULT_ROLE_PROFILE,
                roleCode,
                true,
                primaryRole);

        role.setFhirCodeJson(isBlank(fhirCodeJson) ? null : fhirCodeJson);
        role.setFhirSpecialtyJson(isBlank(fhirSpecialtyJson) ? null : fhirSpecialtyJson);
        role.setFhirTelecomJson(isBlank(fhirTelecomJson) ? null : fhirTelecomJson);
        role.setFhirAvailableTimeJson(isBlank(fhirAvailableTimeJson) ? null : fhirAvailableTimeJson);
        if (periodStart != null) {
            role.setPeriodStart(periodStart);
        }
        if (periodEnd != null) {
            role.setPeriodEnd(periodEnd);
        }

        return toResult(practitionerRoleRepository.save(role));
    }

    public PractitionerRoleResult update(UUID tenantId,
                                         UUID roleId,
                                         String roleCode,
                                         Boolean active,
                                         Boolean primaryRole,
                                         Instant periodStart,
                                         Instant periodEnd,
                                         String fhirCodeJson,
                                         String fhirSpecialtyJson,
                                         String fhirTelecomJson,
                                         String fhirAvailableTimeJson) {
        PractitionerRole role = practitionerRoleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Practitioner role not found: " + roleId));

        if (!isBlank(roleCode)) {
            role.setRoleCode(roleCode.trim());
        }
        if (active != null) {
            role.setActive(active);
        }
        if (primaryRole != null) {
            role.setPrimaryRole(primaryRole);
        }
        if (periodStart != null) {
            role.setPeriodStart(periodStart);
        }
        if (periodEnd != null) {
            role.setPeriodEnd(periodEnd);
        }
        if (fhirCodeJson != null) {
            role.setFhirCodeJson(fhirCodeJson);
        }
        if (fhirSpecialtyJson != null) {
            role.setFhirSpecialtyJson(fhirSpecialtyJson);
        }
        if (fhirTelecomJson != null) {
            role.setFhirTelecomJson(fhirTelecomJson);
        }
        if (fhirAvailableTimeJson != null) {
            role.setFhirAvailableTimeJson(fhirAvailableTimeJson);
        }

        return toResult(practitionerRoleRepository.save(role));
    }

    public PractitionerRoleResult deactivate(UUID tenantId, UUID roleId) {
        PractitionerRole role = practitionerRoleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Practitioner role not found: " + roleId));

        role.setActive(false);
        return toResult(practitionerRoleRepository.save(role));
    }

    private void validateOrganizationInTenant(UUID organizationId, UUID tenantId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + organizationId));

        if (!tenantId.equals(organization.getTenantId())) {
            throw new IllegalArgumentException("Organization does not belong to tenant");
        }
    }

    private PractitionerRoleResult toResult(PractitionerRole role) {
        return new PractitionerRoleResult(
                role.getId(),
                role.getTenantId(),
                role.getOrganizationId(),
                role.getLocationId(),
                role.getPractitionerId(),
                role.getRoleCode(),
                role.isActive(),
                role.isPrimaryRole(),
                role.getPeriodStart(),
                role.getPeriodEnd(),
                role.getFhirCodeJson(),
                role.getFhirSpecialtyJson(),
                role.getFhirTelecomJson(),
                role.getFhirAvailableTimeJson());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record PractitionerRoleResult(
            UUID id,
            UUID tenantId,
            UUID organizationId,
            UUID locationId,
            UUID practitionerId,
            String roleCode,
            boolean active,
            boolean primaryRole,
            Instant periodStart,
            Instant periodEnd,
            String fhirCodeJson,
            String fhirSpecialtyJson,
            String fhirTelecomJson,
            String fhirAvailableTimeJson) {
    }
}
