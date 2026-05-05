package com.clinicadigital.iam.application;

import java.util.UUID;

/**
 * Value object returned by {@link CreateTenantAdminService#create}.
 *
 * Refs: FR-003, FR-022
 *
 * @param tenantId           UUID of the newly created tenant
 * @param adminPractitionerId UUID of the admin's Practitioner record
 * @param auditEventId       ID of the persisted IamAuditEvent (type=TENANT_ADMIN_CREATED)
 */
public record CreateTenantAdminResult(UUID tenantId, UUID adminPractitionerId, UUID auditEventId) {
}
