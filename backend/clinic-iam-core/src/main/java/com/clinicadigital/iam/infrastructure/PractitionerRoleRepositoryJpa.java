package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.PractitionerRole;
import com.clinicadigital.iam.domain.PractitionerRoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class PractitionerRoleRepositoryJpa implements PractitionerRoleRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PractitionerRole> findActiveByPractitionerId(UUID practitionerId) {
        return entityManager.createQuery(
                        "SELECT r FROM PractitionerRole r WHERE r.practitionerId = :practitionerId AND r.active = true",
                        PractitionerRole.class)
                .setParameter("practitionerId", practitionerId)
                .getResultList();
    }

    @Override
    public List<PractitionerRole> findActiveByPractitionerIdAndTenantId(UUID practitionerId, UUID tenantId) {
        return entityManager.createQuery(
                        "SELECT r FROM PractitionerRole r " +
                                "WHERE r.practitionerId = :practitionerId " +
                                "AND r.tenantId = :tenantId " +
                                "AND r.active = true",
                        PractitionerRole.class)
                .setParameter("practitionerId", practitionerId)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }

    @Override
    public Optional<PractitionerRole> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(PractitionerRole.class, id));
    }

    @Override
    public PractitionerRole save(PractitionerRole role) {
        if (entityManager.find(PractitionerRole.class, role.getId()) == null) {
            entityManager.persist(role);
            return role;
        }
        return entityManager.merge(role);
    }
}
