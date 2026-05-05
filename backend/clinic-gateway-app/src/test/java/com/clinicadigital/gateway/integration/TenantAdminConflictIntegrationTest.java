package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.application.AdminEmailAlreadyExistsException;
import com.clinicadigital.iam.application.CreateTenantAdminService;
import com.clinicadigital.iam.application.TenantAlreadyExistsException;
import com.clinicadigital.iam.test.BaseIAMTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T031 [P] [US2] Integration test: conflict detection for nome/CNES/email.
 *
 * Verifies that:
 * <ul>
 *   <li>Attempting to create a tenant with an already-registered CNES throws
 *       {@link TenantAlreadyExistsException}.</li>
 *   <li>Attempting to create a tenant whose organization display_name already exists
 *       throws {@link TenantAlreadyExistsException}.</li>
 *   <li>Attempting to create a tenant whose admin email is already registered as
 *       admin (profile 10) in any other tenant throws
 *       {@link AdminEmailAlreadyExistsException}.</li>
 * </ul>
 *
 * Independent test: runs against a real PostgreSQL container (Testcontainers).
 * Refs: FR-003, FR-009, FR-022
 */
class TenantAdminConflictIntegrationTest extends BaseIAMTest {

    @Autowired
    private CreateTenantAdminService createTenantAdminService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTenantData() {
        // Clean in reverse FK order to avoid constraint violations
        jdbcTemplate.update("DELETE FROM iam_users WHERE profile = 10");
        jdbcTemplate.update("DELETE FROM practitioners WHERE tenant_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM organizations WHERE TRUE");
        // Remove all non-system tenants
        jdbcTemplate.update("DELETE FROM tenants WHERE id != ?", SYSTEM_TENANT_ID);
    }

    @Test
    void createTenantWithNewCnesAndEmailSucceeds() {
        var result = assertDoesNotThrow(() ->
                createTenantAdminService.create(
                        "Clinica Central",
                        "1234567",
                        "Admin Silva",
                        "admin@clinica-central.local",
                        "12345678901",
                        "S3nha!Forte"));

        assertTrue(result.tenantId() != null, "tenantId must be set");
        assertTrue(result.adminPractitionerId() != null, "adminPractitionerId must be set");
        assertTrue(result.auditEventId() != null, "auditEventId must be set");
    }

    @Test
    void duplicateCnesThrowsTenantAlreadyExistsException() {
        createTenantAdminService.create(
                "Clinica Central",
                "1234567",
                "Admin Silva",
                "admin@clinica-central.local",
                "12345678901",
                "S3nha!Forte");

        TenantAlreadyExistsException ex = assertThrows(
                TenantAlreadyExistsException.class,
                () -> createTenantAdminService.create(
                        "Clinica Nova",
                        "1234567",  // same CNES
                        "Admin Costa",
                        "admin@clinica-nova.local",
                        "98765432100",
                        "S3nha!Forte"),
                "duplicate CNES must throw TenantAlreadyExistsException");

        assertTrue(ex.getMessage().toLowerCase().contains("cnes"),
                "exception message must mention cnes conflict");
    }

    @Test
    void duplicateOrganizationNameThrowsTenantAlreadyExistsException() {
        createTenantAdminService.create(
                "Clinica Central",
                "1234567",
                "Admin Silva",
                "admin@clinica-central.local",
                "12345678901",
                "S3nha!Forte");

        TenantAlreadyExistsException ex = assertThrows(
                TenantAlreadyExistsException.class,
                () -> createTenantAdminService.create(
                        "Clinica Central",  // same display_name
                        "7654321",
                        "Admin Costa",
                        "admin@clinica-nova.local",
                        "98765432100",
                        "S3nha!Forte"),
                "duplicate organization name must throw TenantAlreadyExistsException");

        assertTrue(ex.getMessage().toLowerCase().contains("name") ||
                   ex.getMessage().toLowerCase().contains("nome"),
                "exception message must mention name conflict");
    }

    @Test
    void duplicateAdminEmailThrowsAdminEmailAlreadyExistsException() {
        createTenantAdminService.create(
                "Clinica Central",
                "1234567",
                "Admin Silva",
                "admin@shared.local",
                "12345678901",
                "S3nha!Forte");

        assertThrows(
                AdminEmailAlreadyExistsException.class,
                () -> createTenantAdminService.create(
                        "Clinica Nova",
                        "7654321",
                        "Admin Costa",
                        "admin@shared.local",  // same email
                        "98765432100",
                        "S3nha!Forte"),
                "duplicate admin email must throw AdminEmailAlreadyExistsException");
    }

    @Test
    void invalidCnesFormatThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createTenantAdminService.create(
                        "Clinica Central",
                        "123",  // invalid: less than 7 digits
                        "Admin Silva",
                        "admin@clinica-central.local",
                        "12345678901",
                        "S3nha!Forte"),
                "CNES with fewer than 7 digits must be rejected");
    }
}
