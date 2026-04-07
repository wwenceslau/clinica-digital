package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IIamSessionRepository;
import com.clinicadigital.iam.domain.IamSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Stub for TDD compilation — Phase 5.A (T086).
 * Real implementation is in T099 (Phase 5.B).
 *
 * Manages cryptographically opaque session tokens scoped to a tenant.
 * Refs: FR-007, FR-010a
 */
@Service
public class SessionManager {

    private static final long DEFAULT_SESSION_TTL_SECONDS = 30 * 60;

    private final IIamSessionRepository repository;

    public SessionManager(IIamSessionRepository repository) {
        this.repository = repository;
    }

    /**
     * Create and persist a new session for the given user within the given tenant.
     *
     * @param userId   authenticated user
     * @param tenantId tenant scope (enforced via RLS)
     * @param traceId  propagated trace identifier for observability
     * @return persisted IamSession with generated UUID token
     */
    public IamSession createSession(UUID userId, UUID tenantId, String traceId) {
        requireNonNull(userId, "userId");
        requireNonNull(tenantId, "tenantId");

        Instant now = Instant.now();
        IamSession session = new IamSession(
                UUID.randomUUID(),
                tenantId,
                userId,
                now,
                now.plusSeconds(DEFAULT_SESSION_TTL_SECONDS),
                null,
                traceId
        );
        return repository.save(session);
    }

    /**
     * Validate a session by id, checking active status and correct tenant scope.
     *
     * @return true iff session exists, is not expired, is not revoked, and belongs to tenantId
     */
    public boolean validateSession(UUID sessionId, UUID tenantId) {
        requireNonNull(sessionId, "sessionId");
        requireNonNull(tenantId, "tenantId");
        return repository.findById(sessionId)
                .map(session -> tenantId.equals(session.tenantId()) && session.isActive())
                .orElse(false);
    }

    /**
     * Immediately revoke a session so subsequent validation calls return false.
     *
     * @param sessionId session token to revoke
     * @param tenantId  tenant scope guard (prevents cross-tenant revocation)
     */
    public void revokeSession(UUID sessionId, UUID tenantId) {
        requireNonNull(sessionId, "sessionId");
        requireNonNull(tenantId, "tenantId");
        repository.revoke(sessionId, tenantId);
    }

    /**
     * Load a session by id or fail fast when it does not exist.
     */
    public IamSession findRequiredSession(UUID sessionId) {
        requireNonNull(sessionId, "sessionId");
        return repository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionId));
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
