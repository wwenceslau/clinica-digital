package com.clinicadigital.iam.domain;

import java.util.List;
import java.util.UUID;

public interface IamAuditEventRepository {

    IamAuditEvent save(IamAuditEvent event);

    /**
     * Returns audit events for a given tenant, ordered by creation date descending.
     * Returns at most {@code limit} rows.
     */
    List<IamAuditEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, int limit);
}
