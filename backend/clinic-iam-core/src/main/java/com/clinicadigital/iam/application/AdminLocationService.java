package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.Location;
import com.clinicadigital.iam.domain.LocationRepository;
import com.clinicadigital.iam.domain.Organization;
import com.clinicadigital.iam.domain.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Administrative CRUD for tenant locations.
 *
 * Refs: FR-018, FR-019, FR-020
 */
@Service
@Transactional
public class AdminLocationService {

    private static final String DEFAULT_LOCATION_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";

    private final LocationRepository locationRepository;
    private final OrganizationRepository organizationRepository;

    public AdminLocationService(LocationRepository locationRepository,
                                OrganizationRepository organizationRepository) {
        this.locationRepository = locationRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResult> listByTenant(UUID tenantId) {
        return locationRepository.findByTenantId(tenantId).stream()
                .map(this::toResult)
                .toList();
    }

    public LocationResult create(UUID tenantId,
                                 UUID organizationId,
                                 String displayName,
                                 String fhirName,
                                 String fhirStatus,
                                 String fhirMode,
                                 String fhirTelecomJson,
                                 String fhirAddressJson) {
        validateOrganizationInTenant(organizationId, tenantId);

        UUID id = UUID.randomUUID();
        String effectiveName = isBlank(fhirName) ? displayName : fhirName.trim();
        String effectiveStatus = isBlank(fhirStatus) ? "active" : fhirStatus.trim();
        String effectiveMode = isBlank(fhirMode) ? "instance" : fhirMode.trim();

        Location location = new Location(
                id,
                tenantId,
                organizationId,
                "loc-" + id,
                DEFAULT_LOCATION_PROFILE,
                "[{\"system\":\"urn:ietf:rfc:3986\",\"value\":\"urn:uuid:" + id + "\"}]",
                effectiveName,
                effectiveStatus,
                effectiveMode,
                displayName.trim());

        if (!isBlank(fhirTelecomJson)) {
            location.setFhirTelecomJson(fhirTelecomJson.trim());
        }
        if (!isBlank(fhirAddressJson)) {
            location.setFhirAddressJson(fhirAddressJson.trim());
        }

        return toResult(locationRepository.save(location));
    }

    public LocationResult update(UUID tenantId,
                                 UUID locationId,
                                 String displayName,
                                 String fhirName,
                                 String fhirStatus,
                                 String fhirMode,
                                 Boolean accountActive,
                                 String fhirTelecomJson,
                                 String fhirAddressJson) {
        Location location = locationRepository.findByIdAndTenantId(locationId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Location not found: " + locationId));

        if (!isBlank(displayName)) {
            location.setDisplayName(displayName.trim());
        }
        if (!isBlank(fhirName)) {
            location.setFhirName(fhirName.trim());
        }
        if (!isBlank(fhirStatus)) {
            location.setFhirStatus(fhirStatus.trim());
        }
        if (!isBlank(fhirMode)) {
            location.setFhirMode(fhirMode.trim());
        }
        if (accountActive != null) {
            location.setAccountActive(accountActive);
        }
        if (!isBlank(fhirTelecomJson)) {
            location.setFhirTelecomJson(fhirTelecomJson.trim());
        }
        if (!isBlank(fhirAddressJson)) {
            location.setFhirAddressJson(fhirAddressJson.trim());
        }

        return toResult(locationRepository.save(location));
    }

    public LocationResult deactivate(UUID tenantId, UUID locationId) {
        Location location = locationRepository.findByIdAndTenantId(locationId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Location not found: " + locationId));

        location.setAccountActive(false);
        location.setFhirStatus("inactive");
        return toResult(locationRepository.save(location));
    }

    private void validateOrganizationInTenant(UUID organizationId, UUID tenantId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + organizationId));

        if (!tenantId.equals(organization.getTenantId())) {
            throw new IllegalArgumentException("Organization does not belong to tenant");
        }
    }

    private LocationResult toResult(Location location) {
        return new LocationResult(
                location.getId(),
                location.getTenantId(),
                location.getOrganizationId(),
                location.getDisplayName(),
                location.getFhirName(),
                location.getFhirStatus(),
                location.getFhirMode(),
                location.isAccountActive(),
                location.getFhirTelecomJson(),
                location.getFhirAddressJson());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record LocationResult(
            UUID id,
            UUID tenantId,
            UUID organizationId,
            String displayName,
            String fhirName,
            String fhirStatus,
            String fhirMode,
            boolean accountActive,
            String fhirTelecomJson,
            String fhirAddressJson) {
    }
}
