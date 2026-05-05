package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamAuditEvent;
import com.clinicadigital.iam.domain.IamAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final IamAuditEventRepository repository;

    public AuditService(IamAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logAuthEvent(UUID tenantId,
                             UUID actorUserId,
                             String eventType,
                             String outcome,
                             String traceId,
                             String metadataJson) {
        // tenantId is nullable: system-level events (e.g. SUPER_USER_BOOTSTRAPPED) have no tenant.
        repository.save(new IamAuditEvent(tenantId, actorUserId, eventType, outcome, traceId, metadataJson));
    }

    /**
     * Same as {@link #logAuthEvent} but returns the auto-generated audit event ID.
     * Use when the caller needs to include the ID in its response.
     */
    @Transactional
    public UUID logAuthEventReturningId(UUID tenantId,
                                        UUID actorUserId,
                                        String eventType,
                                        String outcome,
                                        String traceId,
                                        String metadataJson) {
        // tenantId is nullable: system-level events have no tenant.
        IamAuditEvent saved = repository.save(
                new IamAuditEvent(tenantId, actorUserId, eventType, outcome, traceId, metadataJson));
        return saved.getId();
    }

    /**
     * Persists an audit event in a brand-new independent transaction.
     *
     * <p>Use this when the audit event must be committed even if the caller's
     * outer transaction is rolled back (e.g. recording a failed bootstrap attempt).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditInNewTransaction(UUID tenantId,
                                         UUID actorUserId,
                                         String eventType,
                                         String outcome,
                                         String traceId,
                                         String metadataJson) {
        // tenantId is nullable: system-level events have no tenant.
        repository.save(new IamAuditEvent(tenantId, actorUserId, eventType, outcome, traceId, metadataJson));
    }
}

