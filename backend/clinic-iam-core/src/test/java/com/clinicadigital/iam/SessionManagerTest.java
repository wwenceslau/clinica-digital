package com.clinicadigital.iam;

import com.clinicadigital.iam.application.SessionManager;
import com.clinicadigital.iam.domain.IIamSessionRepository;
import com.clinicadigital.iam.domain.IamSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private IIamSessionRepository repository;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(IIamSessionRepository.class);
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
                UUID.randomUUID(), tenantId, userId,
                Instant.now(), Instant.now().plusSeconds(1800), null, traceId);
        when(repository.save(any())).thenReturn(expected);

        IamSession result = sessionManager.createSession(userId, tenantId, traceId);

        assertNotNull(result, "session must not be null");
        assertEquals(tenantId, result.tenantId(), "session must be scoped to the correct tenant (Art. 0)");
        assertEquals(userId, result.userId(), "session must reference the authenticated user");
        assertNotNull(result.id(), "session must have a generated UUID token");
        assertNotNull(result.expiresAt(), "session must carry an expiry timestamp (FR-007)");
        assertNotNull(result.traceId(), "session must carry trace_id for observability (FR-010a)");
    }

    @Test
    void createSessionPersistsSessionViaRepository() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        IamSession persisted = new IamSession(
                UUID.randomUUID(), tenantId, userId,
                Instant.now(), Instant.now().plusSeconds(1800), null, "trace-x");
        when(repository.save(any())).thenReturn(persisted);

        sessionManager.createSession(userId, tenantId, "trace-x");

        verify(repository, times(1)).save(any(IamSession.class));
    }

    // -----------------------------------------------------------------------
    // validateSession
    // -----------------------------------------------------------------------

    @Test
    void validateSessionReturnsTrueForActiveSession() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        IamSession active = activeSession(sessionId, tenantId);
        when(repository.findById(sessionId)).thenReturn(Optional.of(active));

        assertTrue(sessionManager.validateSession(sessionId, tenantId),
                "active session must be valid (FR-007)");
    }

    @Test
    void validateSessionReturnsFalseForExpiredSession() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        IamSession expired = new IamSession(
                sessionId, tenantId, UUID.randomUUID(),
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600), // already past
                null, "trace-abc");
        when(repository.findById(sessionId)).thenReturn(Optional.of(expired));

        assertFalse(sessionManager.validateSession(sessionId, tenantId),
                "expired session must be rejected (FR-007)");
    }

    @Test
    void validateSessionReturnsFalseForRevokedSession() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        IamSession revoked = new IamSession(
                sessionId, tenantId, UUID.randomUUID(),
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(1800),
                Instant.now().minusSeconds(30), // already revoked
                "trace-abc");
        when(repository.findById(sessionId)).thenReturn(Optional.of(revoked));

        assertFalse(sessionManager.validateSession(sessionId, tenantId),
                "revoked session must be rejected (FR-007)");
    }

    @Test
    void validateSessionReturnsFalseForUnknownSession() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(repository.findById(sessionId)).thenReturn(Optional.empty());

        assertFalse(sessionManager.validateSession(sessionId, tenantId),
                "unknown session must not be considered valid (fail-closed)");
    }

    @Test
    void validateSessionReturnsFalseWhenTenantDoesNotMatch() {
        UUID sessionId = UUID.randomUUID();
        UUID ownerTenant = UUID.randomUUID();
        UUID attackerTenant = UUID.randomUUID();

        IamSession session = activeSession(sessionId, ownerTenant);
        when(repository.findById(sessionId)).thenReturn(Optional.of(session));

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

        verify(repository, times(1)).revoke(sessionId, tenantId);
    }

    @Test
    void revokeSessionDoesNotCallRepositoryWithDifferentIds() {
        UUID sessionId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        sessionManager.revokeSession(sessionId, tenantId);

           // Verify exactly one revoke call with the correct pair — any other tenant pairing
           // would constitute a cross-tenant revocation bug (Art. 0).
           verify(repository, times(1)).revoke(sessionId, tenantId);
           verifyNoMoreInteractions(repository);
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private IamSession activeSession(UUID sessionId, UUID tenantId) {
        return new IamSession(
                sessionId, tenantId, UUID.randomUUID(),
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(1800),
                null,
                "trace-" + UUID.randomUUID());
    }
}
