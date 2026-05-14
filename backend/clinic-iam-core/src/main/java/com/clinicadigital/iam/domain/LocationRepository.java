package com.clinicadigital.iam.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository interface for {@link Location}.
 *
 * Used by {@link com.clinicadigital.iam.application.UserContextService}
 * to resolve location display names for the active practitioner context.
 *
 * Refs: FR-019, US11
 */
public interface LocationRepository {

    List<Location> findByTenantId(UUID tenantId);

    Optional<Location> findByIdAndTenantId(UUID id, UUID tenantId);

    Location save(Location location);

    Optional<Location> findById(UUID id);
}
