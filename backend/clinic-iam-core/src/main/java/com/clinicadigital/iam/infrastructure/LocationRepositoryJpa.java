package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.Location;
import com.clinicadigital.iam.domain.LocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class LocationRepositoryJpa implements LocationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Location> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Location.class, id));
    }
}
