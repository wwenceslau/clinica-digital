package com.clinicadigital.shared.api;

import org.springframework.stereotype.Component;

@Component
public class TenantContextHolder {

    private final ThreadLocal<TenantContext> tenantContext = new ThreadLocal<>();

    public void set(TenantContext tenantContext) {
        this.tenantContext.set(tenantContext);
    }

    public TenantContext getRequired() {
        TenantContext current = tenantContext.get();
        if (current == null) {
            throw new IllegalStateException("tenant context not initialized");
        }
        return current;
    }

    public boolean isPresent() {
        return tenantContext.get() != null;
    }

    public void clear() {
        tenantContext.remove();
    }
}