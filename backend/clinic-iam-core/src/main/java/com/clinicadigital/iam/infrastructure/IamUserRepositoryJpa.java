package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.IamUser;
import com.clinicadigital.iam.domain.IamUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

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
    public IamUser save(IamUser user) {
        if (entityManager.find(IamUser.class, user.getId()) == null) {
            entityManager.persist(user);
            return user;
        }
        return entityManager.merge(user);
    }
}
