package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.Practitioner;
import com.clinicadigital.iam.domain.PractitionerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class PractitionerRepositoryJpa implements PractitionerRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Practitioner save(Practitioner practitioner) {
        if (entityManager.find(Practitioner.class, practitioner.getId()) == null) {
            entityManager.persist(practitioner);
            return practitioner;
        }
        return entityManager.merge(practitioner);
    }

    @Override
    public boolean existsByTenantIdIsNull() {
        List<Practitioner> result = entityManager.createQuery(
                        "SELECT p FROM Practitioner p WHERE p.tenantId IS NULL",
                        Practitioner.class)
                .setMaxResults(1)
                .getResultList();
        return !result.isEmpty();
    }

    @Override
    public Optional<Practitioner> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Practitioner.class, id));
    }
}
