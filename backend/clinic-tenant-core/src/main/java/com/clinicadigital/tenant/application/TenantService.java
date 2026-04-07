package com.clinicadigital.tenant.application;

import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.tenant.domain.ITenantRepository;
import com.clinicadigital.tenant.domain.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantService.class);

    private final ITenantRepository tenantRepository;

    public TenantService(ITenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Tenant createTenant(String slug, String legalName, String planTier) {
        Tenant tenant = Tenant.newTenant(slug, legalName, planTier);
        logInfo("tenant.create", "started", tenant.getId(), "creating tenant slug=" + slug);
        tenantRepository.findBySlug(slug)
                .ifPresent(existing -> {
                    logWarn("tenant.create", "failure", existing.getId(), "tenant slug already exists: " + slug);
                    throw new IllegalArgumentException("Tenant slug already exists: " + slug);
                });

        Tenant saved = tenantRepository.save(tenant);
        logInfo("tenant.create", "success", saved.getId(), "tenant created slug=" + saved.getSlug());
        return saved;
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID tenantId) {
        logInfo("tenant.get", "started", tenantId, "loading tenant");
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> {
                    logWarn("tenant.get", "failure", tenantId, "tenant not found");
                    return new IllegalArgumentException("Tenant not found: " + tenantId);
                });
        logInfo("tenant.get", "success", tenantId, "tenant loaded");
        return tenant;
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        UUID tenantId = currentTenantId();
        logInfo("tenant.list", "started", tenantId, "listing tenants for current context");
        List<Tenant> tenants = tenantRepository.findAll();
        logInfo("tenant.list", "success", tenantId, "listed tenants count=" + tenants.size());
        return tenants;
    }

    @Transactional
    public Tenant updateQuota(UUID tenantId, Integer requestsPerMinute, Integer concurrency, Integer storageMb) {
        logInfo("tenant.quota.update", "started", tenantId, "updating tenant quotas");
        Tenant tenant = getTenant(tenantId);
        tenant.updateQuota(requestsPerMinute, concurrency, storageMb);
        Tenant saved = tenantRepository.save(tenant);
        logInfo("tenant.quota.update", "success", tenantId, "tenant quotas updated");
        return saved;
    }

    @Transactional
    public Tenant blockTenant(UUID tenantId) {
        logInfo("tenant.block", "started", tenantId, "blocking tenant");
        Tenant tenant = getTenant(tenantId);
        tenant.block();
        Tenant saved = tenantRepository.save(tenant);
        logInfo("tenant.block", "success", tenantId, "tenant blocked");
        return saved;
    }

    @Transactional
    public Tenant unblockTenant(UUID tenantId) {
        logInfo("tenant.unblock", "started", tenantId, "unblocking tenant");
        Tenant tenant = getTenant(tenantId);
        tenant.unblock();
        Tenant saved = tenantRepository.save(tenant);
        logInfo("tenant.unblock", "success", tenantId, "tenant unblocked");
        return saved;
    }

    private void logInfo(String operation, String outcome, UUID tenantId, String message) {
        withStructuredContext(operation, outcome, tenantId, () -> LOGGER.info(message));
    }

    private void logWarn(String operation, String outcome, UUID tenantId, String message) {
        withStructuredContext(operation, outcome, tenantId, () -> LOGGER.warn(message));
    }

    private void withStructuredContext(String operation, String outcome, UUID tenantId, Runnable runnable) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            if (tenantId != null) {
                MDC.put("tenant_id", tenantId.toString());
            } else if (MDC.get("tenant_id") == null) {
                UUID currentTenantId = currentTenantId();
                if (currentTenantId != null) {
                    MDC.put("tenant_id", currentTenantId.toString());
                }
            }
            if (MDC.get("trace_id") == null || MDC.get("trace_id").isBlank()) {
                MDC.put("trace_id", com.clinicadigital.shared.api.TraceContext.generate().traceId());
            }
            MDC.put("operation", operation);
            MDC.put("outcome", outcome);
            runnable.run();
        } finally {
            if (previousContext == null || previousContext.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousContext);
            }
        }
    }

    private UUID currentTenantId() {
        if (TenantContextStore.get() != null) {
            return TenantContextStore.get().tenantId();
        }
        String tenantId = MDC.get("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return UUID.fromString(tenantId);
    }
}
