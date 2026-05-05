package com.clinicadigital.iam.domain;

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

    Optional<Location> findById(UUID id);
}
