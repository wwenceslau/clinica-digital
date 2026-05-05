package com.clinicadigital.iam.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Organization} entities.
 *
 * All queries are tenant-unaware at the application level: CNES is globally
 * unique; display_name uniqueness is enforced here via case-insensitive check.
 */
public interface OrganizationRepository {

    /**
     * Persist a new Organization or update an existing one.
     *
     * @param organization entity to save
     * @return the saved entity (with timestamps populated)
     */
    Organization save(Organization organization);

    Optional<Organization> findById(UUID id);

    /**
     * Check whether an organization with the given CNES already exists.
     *
     * @param cnes 7-digit CNES string
     * @return {@code true} if a matching row exists
     */
    boolean existsByCnes(String cnes);

    /**
     * Check whether an organization with the given display_name already exists
     * (case-insensitive, global scope — CNES is globally unique so organizations
     * must also have distinct display names globally).
     *
     * @param displayName the proposed organization name
     * @return {@code true} if a matching row exists
     */
    boolean existsByDisplayNameIgnoreCase(String displayName);
}
