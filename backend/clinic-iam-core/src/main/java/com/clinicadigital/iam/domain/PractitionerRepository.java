package com.clinicadigital.iam.domain;

import java.util.Optional;
import java.util.UUID;

public interface PractitionerRepository {

    Practitioner save(Practitioner practitioner);

    /** Returns true if at least one global practitioner (tenant_id IS NULL) exists. */
    boolean existsByTenantIdIsNull();

    Optional<Practitioner> findById(UUID id);
}
