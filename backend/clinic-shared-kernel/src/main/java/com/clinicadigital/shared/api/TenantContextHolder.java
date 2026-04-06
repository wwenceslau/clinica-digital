package com.clinicadigital.shared.api;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class TenantContextHolder {

    private TenantContext tenantContext;

    public void set(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    public TenantContext getRequired() {
        if (tenantContext == null) {
            throw new IllegalStateException("tenant context not initialized");
        }
        return tenantContext;
    }

    public boolean isPresent() {
        return tenantContext != null;
    }

    public void clear() {
        tenantContext = null;
    }
}