package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.IIamSessionRepository;
import com.clinicadigital.iam.domain.IamSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class IamSessionRepositoryJpa implements IIamSessionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public IamSession save(IamSession session) {
        if (entityManager.find(IamSession.class, session.id()) == null) {
            entityManager.persist(session);
            return session;
        }
        return entityManager.merge(session);
    }

    @Override
    public Optional<IamSession> findById(UUID sessionId) {
        return Optional.ofNullable(entityManager.find(IamSession.class, sessionId));
    }

    @Override
    public void revoke(UUID sessionId, UUID tenantId) {
        entityManager.createQuery(
                        "UPDATE IamSession s " +
                                "SET s.revokedAt = :revokedAt, s.active = false " +
                                "WHERE s.id = :sessionId AND (s.tenantId = :tenantId OR s.tenantId IS NULL) AND s.revokedAt IS NULL")
                .setParameter("revokedAt", Instant.now())
                .setParameter("sessionId", sessionId)
                .setParameter("tenantId", tenantId)
                .executeUpdate();
    }

    @Override
    public void updateActivePractitionerRole(UUID sessionId, UUID practitionerRoleId) {
        entityManager.createQuery(
                        "UPDATE IamSession s " +
                                "SET s.activePractitionerRoleId = :practitionerRoleId " +
                                "WHERE s.id = :sessionId")
                .setParameter("practitionerRoleId", practitionerRoleId)
                .setParameter("sessionId", sessionId)
                .executeUpdate();
    }
}
