package com.clinicadigital.tenant.application;

import java.util.UUID;

public class QuotaExceededException extends RuntimeException {

    private final UUID tenantId;
    private final String metric;
    private final int limit;

    public QuotaExceededException(UUID tenantId, String metric, int limit) {
        super("tenant quota exceeded: tenantId=" + tenantId + ", metric=" + metric + ", limit=" + limit);
        this.tenantId = tenantId;
        this.metric = metric;
        this.limit = limit;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getMetric() {
        return metric;
    }

    public int getLimit() {
        return limit;
    }
}