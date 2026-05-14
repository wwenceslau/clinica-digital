package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.IIamSessionRepository;
import com.clinicadigital.iam.domain.IamSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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
    public Optional<IamSession> findByOpaqueTokenDigest(String opaqueTokenDigest) {
        return entityManager.createQuery(
                        "SELECT s FROM IamSession s WHERE s.opaqueTokenDigest = :opaqueTokenDigest",
                        IamSession.class)
                .setParameter("opaqueTokenDigest", opaqueTokenDigest)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

            @Override
            public List<IamSession> findByTenantIdOrderByIssuedAtDesc(UUID tenantId, int limit) {
            int effectiveLimit = Math.max(1, limit);
            return entityManager.createQuery(
                    "SELECT s FROM IamSession s " +
                        "WHERE s.tenantId = :tenantId " +
                        "ORDER BY s.issuedAt DESC",
                    IamSession.class)
                .setParameter("tenantId", tenantId)
                .setMaxResults(effectiveLimit)
                .getResultList();
            }

    @Override
    public void revoke(UUID sessionId, UUID tenantId) {
        // Backward-compatible path: interpret input as token UUID and revoke by digest.
        revokeByOpaqueTokenDigest(sha256Hex(sessionId.toString()), tenantId, "logout");
    }

    @Override
    public void revokeByOpaqueTokenDigest(String opaqueTokenDigest, UUID tenantId, String revocationReason) {
        entityManager.createQuery(
                        "UPDATE IamSession s " +
                                "SET s.revokedAt = :revokedAt, s.active = false, s.revocationReason = :revocationReason " +
                                "WHERE s.opaqueTokenDigest = :opaqueTokenDigest " +
                                "AND (s.tenantId = :tenantId OR s.tenantId IS NULL) AND s.revokedAt IS NULL")
                .setParameter("revokedAt", Instant.now())
                .setParameter("revocationReason", revocationReason)
                .setParameter("opaqueTokenDigest", opaqueTokenDigest)
                .setParameter("tenantId", tenantId)
                .executeUpdate();
    }

    @Override
    public void updateActivePractitionerRole(UUID sessionId, UUID practitionerRoleId) {
        entityManager.createQuery(
                        "UPDATE IamSession s " +
                                "SET s.activePractitionerRoleId = :practitionerRoleId " +
                                "WHERE s.opaqueTokenDigest = :opaqueTokenDigest")
                .setParameter("practitionerRoleId", practitionerRoleId)
                .setParameter("opaqueTokenDigest", sha256Hex(sessionId.toString()))
                .executeUpdate();
    }

    private static String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
