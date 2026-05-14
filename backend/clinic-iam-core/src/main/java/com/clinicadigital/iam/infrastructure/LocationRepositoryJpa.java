package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.Location;
import com.clinicadigital.iam.domain.LocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class LocationRepositoryJpa implements LocationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Location> findByTenantId(UUID tenantId) {
        return entityManager.createQuery(
                        "SELECT l FROM Location l WHERE l.tenantId = :tenantId ORDER BY l.displayName ASC",
                        Location.class)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }

    @Override
    public Optional<Location> findByIdAndTenantId(UUID id, UUID tenantId) {
        return entityManager.createQuery(
                        "SELECT l FROM Location l WHERE l.id = :id AND l.tenantId = :tenantId",
                        Location.class)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Location save(Location location) {
        if (location.getId() == null) {
            entityManager.persist(location);
            return location;
        }
        return entityManager.merge(location);
    }

    @Override
    public Optional<Location> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Location.class, id));
    }
}
