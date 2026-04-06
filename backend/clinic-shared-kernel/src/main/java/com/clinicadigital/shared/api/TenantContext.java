package com.clinicadigital.shared.api;

import java.util.UUID;

/**
 * Tenant context contract used across modules.
 */
public record TenantContext(UUID tenantId) {

	public TenantContext {
		if (tenantId == null) {
			throw new IllegalArgumentException("tenantId must not be null");
		}
	}

	public static TenantContext from(UUID tenantId) {
		return new TenantContext(tenantId);
	}

	public TenantContext withTenantId(UUID newTenantId) {
		return new TenantContext(newTenantId);
	}
}
