package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.Organization;
import com.clinicadigital.iam.domain.OrganizationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of {@link OrganizationRepository}.
 *
 * Follows the same EntityManager-based pattern used in {@link IamUserRepositoryJpa}:
 * JPQL queries, {@code setMaxResults(1)} for existence checks.
 */
@Repository
class OrganizationRepositoryJpa implements OrganizationRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Organization save(Organization organization) {
        return em.merge(organization);
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return Optional.ofNullable(em.find(Organization.class, id));
    }

    @Override
    public boolean existsByCnes(String cnes) {
        return !em.createQuery(
                        "SELECT o FROM Organization o WHERE o.cnes = :cnes",
                        Organization.class)
                .setParameter("cnes", cnes)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    @Override
    public boolean existsByDisplayNameIgnoreCase(String displayName) {
        return !em.createQuery(
                        "SELECT o FROM Organization o WHERE LOWER(o.displayName) = LOWER(:name)",
                        Organization.class)
                .setParameter("name", displayName)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }
}
