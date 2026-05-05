package com.clinicadigital.iam.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository interface for {@link PractitionerRole}.
 *
 * Used during multi-org login resolution to find all active organizations
 * that a practitioner has access to.
 *
 * Refs: US4, FR-020
 */
public interface PractitionerRoleRepository {

    /**
     * Find all active practitioner roles for a given practitioner.
     *
     * @param practitionerId the practitioner FK
     * @return list of active roles (may be empty)
     */
    List<PractitionerRole> findActiveByPractitionerId(UUID practitionerId);

    /**
     * Find all active roles for a practitioner scoped to a specific tenant.
     *
     * @param practitionerId the practitioner FK
     * @param tenantId       tenant scope
     * @return list of active roles (may be empty)
     */
    List<PractitionerRole> findActiveByPractitionerIdAndTenantId(UUID practitionerId, UUID tenantId);

    /**
     * Find a practitioner role by its primary key.
     *
     * @param id the role UUID
     * @return Optional containing the role, or empty if not found
     */
    Optional<PractitionerRole> findById(UUID id);

    PractitionerRole save(PractitionerRole role);
}
