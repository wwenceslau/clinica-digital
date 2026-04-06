package com.clinicadigital.tenant.application;

import com.clinicadigital.tenant.domain.ITenantRepository;
import com.clinicadigital.tenant.domain.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private final ITenantRepository tenantRepository;

    public TenantService(ITenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Tenant createTenant(String slug, String legalName, String planTier) {
        tenantRepository.findBySlug(slug)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tenant slug already exists: " + slug);
                });

        Tenant tenant = Tenant.newTenant(slug, legalName, planTier);
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    @Transactional
    public Tenant updateQuota(UUID tenantId, Integer requestsPerMinute, Integer concurrency, Integer storageMb) {
        Tenant tenant = getTenant(tenantId);
        tenant.updateQuota(requestsPerMinute, concurrency, storageMb);
        return tenantRepository.save(tenant);
    }
}
