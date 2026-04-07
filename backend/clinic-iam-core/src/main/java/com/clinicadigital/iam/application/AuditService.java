package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamAuditEvent;
import com.clinicadigital.iam.domain.IamAuditEventRepository;
import org.springframework.stereotype.Service;
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
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        repository.save(new IamAuditEvent(tenantId, actorUserId, eventType, outcome, traceId, metadataJson));
    }
}
