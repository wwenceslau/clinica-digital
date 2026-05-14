package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IIamSessionRepository;
import com.clinicadigital.iam.domain.IamSession;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        // tenantId is nullable: super-user (profile 0) sessions are not tenant-scoped

        Instant now = Instant.now();
        UUID opaqueToken = UUID.randomUUID();
        IamSession session = new IamSession(
                UUID.randomUUID(),
                tenantId,
            tenantId,
                userId,
                now,
                now.plusSeconds(DEFAULT_SESSION_TTL_SECONDS),
                null,
            traceId,
            sha256Hex(opaqueToken.toString())
        );
        session.setOpaqueToken(opaqueToken);
        return repository.save(session);
    }

    /**
     * Validate a session by id, checking active status and correct tenant scope.
     *
     * @return true iff session exists, is not expired, is not revoked, and belongs to tenantId
     */
    public boolean validateSession(UUID sessionId, UUID tenantId) {
        requireNonNull(sessionId, "sessionId");
        // tenantId is nullable: super-user sessions have no tenant scope
        String digest = sha256Hex(sessionId.toString());
        return repository.findByOpaqueTokenDigest(digest)
                .map(session -> {
                    boolean tenantMatch = tenantId == null
                            ? session.tenantId() == null
                            : tenantId.equals(session.tenantId());
                // Defensive constant-time compare avoids digest oracle timing variance.
                boolean digestMatch = MessageDigest.isEqual(
                    session.opaqueTokenDigest().getBytes(StandardCharsets.UTF_8),
                    digest.getBytes(StandardCharsets.UTF_8));
                return digestMatch && tenantMatch && session.isActive();
                })
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
        // tenantId is nullable: super-user sessions have no tenant scope
        repository.revokeByOpaqueTokenDigest(sha256Hex(sessionId.toString()), tenantId, "logout");
    }

    /**
     * Load a session by id or fail fast when it does not exist.
     */
    public IamSession findRequiredSession(UUID sessionId) {
        requireNonNull(sessionId, "sessionId");
        return repository.findByOpaqueTokenDigest(sha256Hex(sessionId.toString()))
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionId));
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
