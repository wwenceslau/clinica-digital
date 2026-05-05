package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.IamPermission;
import com.clinicadigital.iam.domain.IamPermissionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T102 [US6] JPA implementation of {@link IamPermissionRepository}.
 *
 * Refs: FR-006, data-model.md
 */
@Repository
class IamPermissionRepositoryJpa implements IamPermissionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public IamPermission save(IamPermission permission) {
        if (entityManager.find(IamPermission.class, permission.getId()) == null) {
            entityManager.persist(permission);
            return permission;
        }
        return entityManager.merge(permission);
    }

    @Override
    public Optional<IamPermission> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(IamPermission.class, id));
    }

    @Override
    public Optional<IamPermission> findByCode(String code) {
        return entityManager.createQuery(
                        "SELECT p FROM IamPermission p WHERE p.code = :code",
                        IamPermission.class)
                .setParameter("code", code)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<IamPermission> findAll() {
        return entityManager.createQuery("SELECT p FROM IamPermission p ORDER BY p.code", IamPermission.class)
                .getResultList();
    }
}
