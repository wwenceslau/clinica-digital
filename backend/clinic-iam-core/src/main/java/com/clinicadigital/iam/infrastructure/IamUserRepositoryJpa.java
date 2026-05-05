package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class IamUserRepositoryJpa implements IamUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<IamUser> findByIdAndTenantId(UUID id, UUID tenantId) {
        return entityManager.createQuery(
                        "SELECT u FROM IamUser u WHERE u.id = :id AND u.tenantId = :tenantId",
                        IamUser.class)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<IamUser> findByEmailAndTenantId(String email, UUID tenantId) {
        return entityManager.createQuery(
                        "SELECT u FROM IamUser u WHERE u.email = :email AND u.tenantId = :tenantId",
                        IamUser.class)
                .setParameter("email", email)
                .setParameter("tenantId", tenantId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsByProfile(int profile) {
        List<IamUser> result = entityManager.createQuery(
                        "SELECT u FROM IamUser u WHERE u.profile = :profile",
                        IamUser.class)
                .setParameter("profile", profile)
                .setMaxResults(1)
                .getResultList();
        return !result.isEmpty();
    }

    @Override
    public boolean existsByEmailAndProfile(String email, int profile) {
        List<IamUser> result = entityManager.createQuery(
                        "SELECT u FROM IamUser u WHERE u.email = :email AND u.profile = :profile",
                        IamUser.class)
                .setParameter("email", email)
                .setParameter("profile", profile)
                .setMaxResults(1)
                .getResultList();
        return !result.isEmpty();
    }

    @Override
    public boolean existsByEmailAndTenantId(String email, UUID tenantId) {
        List<IamUser> result = entityManager.createQuery(
                        "SELECT u FROM IamUser u WHERE u.email = :email AND u.tenantId = :tenantId",
                        IamUser.class)
                .setParameter("email", email)
                .setParameter("tenantId", tenantId)
                .setMaxResults(1)
                .getResultList();
        return !result.isEmpty();
    }

    @Override
    public IamUser save(IamUser user) {
        if (entityManager.find(IamUser.class, user.getId()) == null) {
            entityManager.persist(user);
            return user;
        }
        return entityManager.merge(user);
    }

    @Override
    public List<IamUser> findByEmail(String email) {
        return entityManager.createQuery(
                        "SELECT u FROM IamUser u WHERE u.email = :email",
                        IamUser.class)
                .setParameter("email", email)
                .getResultList();
    }

    @Override
    public List<IamUser> findByTenantId(UUID tenantId) {
        return entityManager.createQuery(
                        "SELECT u FROM IamUser u WHERE u.tenantId = :tenantId ORDER BY u.createdAt ASC",
                        IamUser.class)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }
}
