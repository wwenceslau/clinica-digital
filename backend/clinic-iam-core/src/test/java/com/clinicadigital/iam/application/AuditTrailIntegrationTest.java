package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamAuditEvent;
import com.clinicadigital.iam.domain.IamAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * T067 [P] [US9] — Integration test for the audit trail ({@link AuditService} + {@link IamAuditEvent}).
 *
 * <p>Verifies the append-only audit contract (FR-016, FR-024):
 * <ul>
 *   <li>{@code logAuthEvent} delegates exactly one {@code save} to the repository with correct fields.</li>
 *   <li>{@code logAuthEventReturningId} returns a non-null UUID from the persisted event.</li>
 *   <li>{@code logAuditInNewTransaction} delegates exactly one {@code save} to the repository.</li>
 *   <li>Null tenantId is rejected before any {@code save} call.</li>
 *   <li>{@link IamAuditEventRepository} interface exposes only {@code save()} — no delete or update methods.</li>
 *   <li>{@link IamAuditEvent} carries all mandatory fields after construction.</li>
 * </ul>
 *
 * <p>No database required — {@link IamAuditEventRepository} is mocked via Mockito.
 *
 * Refs: FR-016, FR-024, Constitution Art. VI, T070
 */
@ExtendWith(MockitoExtension.class)
class AuditTrailIntegrationTest {

    @Mock
    private IamAuditEventRepository repository;

    private AuditService auditService;

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID ACTOR_ID    = UUID.randomUUID();
    private static final String EVENT_TYPE  = "auth.login";
    private static final String OUTCOME     = "success";
    private static final String TRACE_ID    = "trace-001";
    private static final String METADATA    = "{\"ip\":\"127.0.0.1\"}";

    @BeforeEach
    void setUp() {
        auditService = new AuditService(repository);
    }

    // ── logAuthEvent ──────────────────────────────────────────────────────

    @Test
    void logAuthEvent_callsRepositorySaveExactlyOnce() {
        // Arrange: repository returns the argument unchanged (persist behaviour)
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        auditService.logAuthEvent(TENANT_ID, ACTOR_ID, EVENT_TYPE, OUTCOME, TRACE_ID, METADATA);

        // Assert
        verify(repository, times(1)).save(any(IamAuditEvent.class));
    }

    @Test
    void logAuthEvent_persistsCorrectFields() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<IamAuditEvent> captor = ArgumentCaptor.forClass(IamAuditEvent.class);

        auditService.logAuthEvent(TENANT_ID, ACTOR_ID, EVENT_TYPE, OUTCOME, TRACE_ID, METADATA);

        verify(repository).save(captor.capture());
        IamAuditEvent saved = captor.getValue();

        assertEquals(TENANT_ID,  saved.getTenantId(),    "tenantId must be propagated");
        assertEquals(ACTOR_ID,   saved.getActorUserId(), "actorUserId must be propagated");
        assertEquals(EVENT_TYPE, saved.getEventType(),   "eventType must be propagated");
        assertEquals(OUTCOME,    saved.getOutcome(),     "outcome must be propagated");
        assertEquals(TRACE_ID,   saved.getTraceId(),     "traceId must be propagated");
        assertEquals(METADATA,   saved.getMetadataJson(),"metadataJson must be propagated");
    }

    @Test
    void logAuthEvent_nullTenantId_throwsIllegalArgument_beforeSave() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAuthEvent(null, ACTOR_ID, EVENT_TYPE, OUTCOME, TRACE_ID, null));

        verifyNoInteractions(repository);
    }

    // ── logAuthEventReturningId ───────────────────────────────────────────

    @Test
    void logAuthEventReturningId_returnsNonNullId() {
        UUID expectedId = UUID.randomUUID();
        IamAuditEvent stubEvent = new IamAuditEvent(TENANT_ID, ACTOR_ID, EVENT_TYPE, OUTCOME, TRACE_ID, null);
        // Simulate JPA assigning an ID upon persist
        forceSetId(stubEvent, expectedId);
        when(repository.save(any())).thenReturn(stubEvent);

        UUID returnedId = auditService.logAuthEventReturningId(TENANT_ID, ACTOR_ID, EVENT_TYPE, OUTCOME, TRACE_ID, null);

        assertNotNull(returnedId, "returned audit event ID must not be null");
        assertEquals(expectedId, returnedId, "returned ID must match the persisted event ID");
    }

    @Test
    void logAuthEventReturningId_nullTenantId_throwsIllegalArgument_beforeSave() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAuthEventReturningId(null, ACTOR_ID, EVENT_TYPE, OUTCOME, TRACE_ID, null));

        verifyNoInteractions(repository);
    }

    // ── logAuditInNewTransaction ──────────────────────────────────────────

    @Test
    void logAuditInNewTransaction_callsRepositorySaveExactlyOnce() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.logAuditInNewTransaction(TENANT_ID, ACTOR_ID, "SUPER_USER_BOOTSTRAPPED", "success", TRACE_ID, null);

        verify(repository, times(1)).save(any(IamAuditEvent.class));
    }

    @Test
    void logAuditInNewTransaction_nullTenantId_throwsIllegalArgument_beforeSave() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAuditInNewTransaction(null, ACTOR_ID, EVENT_TYPE, OUTCOME, TRACE_ID, null));

        verifyNoInteractions(repository);
    }

    // ── Repository interface contract (append-only) ───────────────────────

    @Test
    void iamAuditEventRepository_exposes_onlySaveMethod() {
        Method[] methods = IamAuditEventRepository.class.getDeclaredMethods();

        // The interface must declare exactly one method: save()
        assertEquals(1, methods.length,
                "IamAuditEventRepository must expose only save() — no delete or update");
        assertEquals("save", methods[0].getName(),
                "The single method of IamAuditEventRepository must be save()");
    }

    @Test
    void iamAuditEventRepository_saveAcceptsAndReturnsIamAuditEvent() {
        Method[] methods = IamAuditEventRepository.class.getDeclaredMethods();
        Method saveMethod = methods[0];

        assertEquals(IamAuditEvent.class, saveMethod.getReturnType(),
                "save() must return IamAuditEvent");
        assertEquals(1, saveMethod.getParameterCount(),
                "save() must take exactly one parameter");
        assertEquals(IamAuditEvent.class, saveMethod.getParameterTypes()[0],
                "save() parameter must be IamAuditEvent");
    }

    // ── IamAuditEvent entity — allowed null actorUserId for system events ─

    @Test
    void iamAuditEvent_supportsNullActorUserId_forSystemEvents() {
        // System-initiated events (e.g. SUPER_USER_BOOTSTRAPPED) may have no actor
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<IamAuditEvent> captor = ArgumentCaptor.forClass(IamAuditEvent.class);

        auditService.logAuthEvent(TENANT_ID, null, "SUPER_USER_BOOTSTRAPPED", "success", null, null);

        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getActorUserId(),
                "actorUserId may be null for system-generated audit events");
    }

    // ── Utility: set the private UUID id field reflectively ───────────────

    private static void forceSetId(IamAuditEvent event, UUID id) {
        try {
            java.lang.reflect.Field field = IamAuditEvent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(event, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject test UUID into IamAuditEvent.id", e);
        }
    }
}
