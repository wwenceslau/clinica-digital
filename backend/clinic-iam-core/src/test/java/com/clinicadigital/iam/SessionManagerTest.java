package com.clinicadigital.iam;

import com.clinicadigital.iam.application.SessionManager;
import com.clinicadigital.iam.domain.IIamSessionRepository;
import com.clinicadigital.iam.domain.IamSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T086 [US3] — Unit test for SessionManager (create, validate, revoke).
 *
 * <p><b>TDD state</b>: RED until {@link SessionManager} (T099, Phase 5.B) is
 * implemented.
 *
 * <p>Contract verified:
 * <ul>
 *   <li>createSession: returns IamSession scoped to the given tenant and user.</li>
 *   <li>validateSession: returns {@code true} for active, non-expired, non-revoked session
 *       with matching tenant.</li>
 *   <li>validateSession: returns {@code false} for expired sessions.</li>
 *   <li>validateSession: returns {@code false} for revoked sessions.</li>
 *   <li>validateSession: returns {@code false} for unknown session ids.</li>
 *   <li>validateSession: returns {@code false} when session belongs to a different tenant
 *       (cross-tenant isolation).</li>
 *   <li>revokeSession: delegates to repository.revoke with correct session and tenant ids.</li>
 * </ul>
 *
 * Refs: FR-007, FR-010a, Art. 0 (tenant-scoped session validation)
 */
class SessionManagerTest {

    private InMemorySessionRepository repository;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        repository = new InMemorySessionRepository();
        sessionManager = new SessionManager(repository);
    }

    // -----------------------------------------------------------------------
    // createSession
    // -----------------------------------------------------------------------

    @Test
    void createSessionReturnsSessionScopedToTenantAndUser() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String traceId = "trace-" + UUID.randomUUID();

        IamSession expected = new IamSession(
            UUID.randomUUID(), tenantId, tenantId, userId,
            Instant.now(), Instant.now().plusSeconds(1800), null, traceId, "digest");
        expected.setOpaqueToken(UUID.randomUUID());
        repository.saveResult = expected;

        IamSession result = sessionManager.createSession(userId, tenantId, traceId);

        assertNotNull(result, "session must not be null");
        assertEquals(tenantId, result.tenantId(), "session must be scoped to the correct tenant (Art. 0)");
        assertEquals(userId, result.userId(), "session must reference the authenticated user");
        assertNotNull(result.opaqueToken(), "session must expose an opaque token to the client");
        assertNotNull(result.id(), "session must have a generated DB identifier");
        assertNotNull(result.expiresAt(), "session must carry an expiry timestamp (FR-007)");
        assertNotNull(result.traceId(), "session must carry trace_id for observability (FR-010a)");
    }

    @Test
    void createSessionPersistsSessionViaRepository() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        IamSession persisted = new IamSession(
            UUID.randomUUID(), tenantId, tenantId, userId,
            Instant.now(), Instant.now().plusSeconds(1800), null, "trace-x", "digest");
        persisted.setOpaqueToken(UUID.randomUUID());
        repository.saveResult = persisted;

        sessionManager.createSession(userId, tenantId, "trace-x");

        assertEquals(1, repository.saveCalls, "save must be called exactly once");
    }

    // -----------------------------------------------------------------------
    // validateSession
    // -----------------------------------------------------------------------

    @Test
    void validateSessionReturnsTrueForActiveSession() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        IamSession active = activeSession(sessionId, tenantId);
        repository.byDigest.put(sha256Hex(sessionId), active);

        assertTrue(sessionManager.validateSession(sessionId, tenantId),
                "active session must be valid (FR-007)");
    }

    @Test
    void validateSessionReturnsFalseForExpiredSession() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        IamSession expired = new IamSession(
            UUID.randomUUID(), tenantId, tenantId, UUID.randomUUID(),
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600), // already past
            null, "trace-abc", sha256Hex(sessionId));
        repository.byDigest.put(sha256Hex(sessionId), expired);

        assertFalse(sessionManager.validateSession(sessionId, tenantId),
                "expired session must be rejected (FR-007)");
    }

    @Test
    void validateSessionReturnsFalseForRevokedSession() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        IamSession revoked = new IamSession(
            UUID.randomUUID(), tenantId, tenantId, UUID.randomUUID(),
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(1800),
                Instant.now().minusSeconds(30), // already revoked
            "trace-abc", sha256Hex(sessionId));
        repository.byDigest.put(sha256Hex(sessionId), revoked);

        assertFalse(sessionManager.validateSession(sessionId, tenantId),
                "revoked session must be rejected (FR-007)");
    }

    @Test
    void validateSessionReturnsFalseForUnknownSession() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        repository.byDigest.remove(sha256Hex(sessionId));

        assertFalse(sessionManager.validateSession(sessionId, tenantId),
                "unknown session must not be considered valid (fail-closed)");
    }

    @Test
    void validateSessionReturnsFalseWhenTenantDoesNotMatch() {
        UUID sessionId = UUID.randomUUID();
        UUID ownerTenant = UUID.randomUUID();
        UUID attackerTenant = UUID.randomUUID();

        IamSession session = activeSession(sessionId, ownerTenant);
        repository.byDigest.put(sha256Hex(sessionId), session);

        assertFalse(sessionManager.validateSession(sessionId, attackerTenant),
                "session tenant mismatch must be rejected — cross-tenant isolation (Art. 0)");
    }

    // -----------------------------------------------------------------------
    // revokeSession
    // -----------------------------------------------------------------------

    @Test
    void revokeSessionInvokesRepositoryWithCorrectIds() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        sessionManager.revokeSession(sessionId, tenantId);

        assertEquals(1, repository.revokeCalls, "revoke must be called exactly once");
        assertEquals(sha256Hex(sessionId), repository.lastRevokedDigest);
        assertEquals(tenantId, repository.lastRevokedTenantId);
        assertEquals("logout", repository.lastRevocationReason);
    }

    @Test
    void revokeSessionDoesNotCallRepositoryWithDifferentIds() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        sessionManager.revokeSession(sessionId, tenantId);

        // Verify exactly one revoke call with the correct pair; any different pairing
        // would indicate a cross-tenant revocation bug (Art. 0).
        assertEquals(1, repository.revokeCalls);
        assertEquals(sha256Hex(sessionId), repository.lastRevokedDigest);
        assertEquals(tenantId, repository.lastRevokedTenantId);
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private IamSession activeSession(UUID sessionId, UUID tenantId) {
        return new IamSession(
                UUID.randomUUID(), tenantId, tenantId, UUID.randomUUID(),
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(1800),
                null,
                "trace-" + UUID.randomUUID(),
                sha256Hex(sessionId));
    }

    private static String sha256Hex(UUID token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static final class InMemorySessionRepository implements IIamSessionRepository {
        private final Map<String, IamSession> byDigest = new HashMap<>();
        private IamSession saveResult;
        private int saveCalls;
        private int revokeCalls;
        private String lastRevokedDigest;
        private UUID lastRevokedTenantId;
        private String lastRevocationReason;

        @Override
        public IamSession save(IamSession session) {
            saveCalls++;
            IamSession persisted = saveResult != null ? saveResult : session;
            if (persisted.opaqueTokenDigest() != null) {
                byDigest.put(persisted.opaqueTokenDigest(), persisted);
            }
            return persisted;
        }

        @Override
        public Optional<IamSession> findById(UUID sessionId) {
            return Optional.empty();
        }

        @Override
        public Optional<IamSession> findByOpaqueTokenDigest(String opaqueTokenDigest) {
            return Optional.ofNullable(byDigest.get(opaqueTokenDigest));
        }

        @Override
        public void revoke(UUID sessionId, UUID tenantId) {
            throw new UnsupportedOperationException("Not used in SessionManager tests");
        }

        @Override
        public void revokeByOpaqueTokenDigest(String opaqueTokenDigest, UUID tenantId, String revocationReason) {
            revokeCalls++;
            lastRevokedDigest = opaqueTokenDigest;
            lastRevokedTenantId = tenantId;
            lastRevocationReason = revocationReason;
        }

        @Override
        public void updateActivePractitionerRole(UUID sessionId, UUID practitionerRoleId) {
            throw new UnsupportedOperationException("Not used in SessionManager tests");
        }

        @Override
        public List<IamSession> findByTenantIdOrderByIssuedAtDesc(UUID tenantId, int limit) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findByTenantIdOrderByIssuedAtDesc'");
        }
    }
}
