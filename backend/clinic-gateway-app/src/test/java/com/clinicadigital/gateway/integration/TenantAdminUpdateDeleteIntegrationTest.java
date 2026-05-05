package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.test.BaseIAMTest;
import com.clinicadigital.tenant.application.TenantService;
import com.clinicadigital.tenant.domain.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T151/T152 [US2] Integration coverage for tenant update/delete persistence.
 *
 * Verifies that update and delete operations used by AdminTenantController
 * persist correctly in PostgreSQL and enforce conflict/not-found rules.
 *
 * Refs: FR-003, FR-009
 */
class TenantAdminUpdateDeleteIntegrationTest extends BaseIAMTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTenants() {
        jdbcTemplate.update("DELETE FROM tenants WHERE id != ?", SYSTEM_TENANT_ID);
    }

    @Test
    void updateTenantMustPersistSlugLegalNameAndPlanTier() {
        Tenant created = tenantService.createTenant("tenant-alpha", "Tenant Alpha", "starter");

        Tenant updated = tenantService.updateTenant(
                created.getId(),
                "tenant-alpha-v2",
                "Tenant Alpha V2",
                "growth");

        assertEquals("tenant-alpha-v2", updated.getSlug());
        assertEquals("Tenant Alpha V2", updated.getLegalName());
        assertEquals("growth", updated.getPlanTier());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT slug, legal_name, plan_tier FROM tenants WHERE id = ?",
                created.getId());

        assertEquals("tenant-alpha-v2", row.get("slug"));
        assertEquals("Tenant Alpha V2", row.get("legal_name"));
        assertEquals("growth", row.get("plan_tier"));
    }

    @Test
    void updateTenantWithDuplicateSlugMustFail() {
        Tenant first = tenantService.createTenant("tenant-a", "Tenant A", "starter");
        Tenant second = tenantService.createTenant("tenant-b", "Tenant B", "starter");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.updateTenant(second.getId(), first.getSlug(), "Tenant B2", "growth"));

        assertTrue(ex.getMessage().toLowerCase().contains("slug"));
    }

    @Test
    void deleteTenantMustRemoveTenantRecord() {
        Tenant created = tenantService.createTenant("tenant-z", "Tenant Z", "starter");

        tenantService.deleteTenant(created.getId());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?",
                Integer.class,
                created.getId());

        assertEquals(0, count);
    }

    @Test
    void deletingUnknownTenantMustFailWithNotFoundBehavior() {
        UUID unknownTenantId = UUID.randomUUID();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.deleteTenant(unknownTenantId));

        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }
}
