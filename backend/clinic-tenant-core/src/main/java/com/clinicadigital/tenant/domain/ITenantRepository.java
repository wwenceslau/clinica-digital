package com.clinicadigital.tenant.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITenantRepository {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findById(UUID id);

    List<Tenant> findAll();

    Tenant save(Tenant tenant);
}
