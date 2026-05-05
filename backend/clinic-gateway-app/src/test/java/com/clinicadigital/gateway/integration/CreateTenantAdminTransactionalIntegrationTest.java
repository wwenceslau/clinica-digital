package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.application.CreateTenantAdminResult;
import com.clinicadigital.iam.application.CreateTenantAdminService;
import com.clinicadigital.iam.test.BaseIAMTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T032 [P] [US2] Integration test: transactional creation of Organization +
 * Practitioner + IamUser (admin, profile=10).
 *
 * Verifies that:
 * <ul>
 *   <li>A successful call creates all three entities atomically and returns
 *       the correct result record.</li>
 *   <li>The persisted organization has the correct CNES, display_name and RNDS
 *       FHIR profile.</li>
 *   <li>The persisted iam_user has profile=10 and is linked to the practitioner.</li>
 *   <li>An audit event of type TENANT_ADMIN_CREATED is persisted with outcome success.</li>
 * </ul>
 *
 * Refs: FR-003, FR-010, FR-022
 */
class CreateTenantAdminTransactionalIntegrationTest extends BaseIAMTest {

    private static final String RNDS_ORG_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude";
    private static final String RNDS_PRACTITIONER_PROFILE =
            "http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude";

    @Autowired
    private CreateTenantAdminService createTenantAdminService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTenantData() {
        jdbcTemplate.update("DELETE FROM iam_users WHERE profile = 10");
        jdbcTemplate.update("DELETE FROM practitioners WHERE tenant_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM organizations WHERE TRUE");
        jdbcTemplate.update("DELETE FROM tenants WHERE id != ?", SYSTEM_TENANT_ID);
    }

    @Test
    void createSuccessfullyReturnsTenantIdAndAdminPractitionerId() {
        CreateTenantAdminResult result = createTenantAdminService.create(
                "Clinica Esperanca",
                "1234567",
                "Admin Esperanca",
                "admin@esperanca.local",
                "12345678901",
                "S3nha!Forte");

        assertAll(
                "service result must be complete",
                () -> assertNotNull(result.tenantId(), "tenantId must not be null"),
                () -> assertNotNull(result.adminPractitionerId(), "adminPractitionerId must not be null"),
                () -> assertNotNull(result.auditEventId(), "auditEventId must not be null")
        );
    }

    @Test
    void organizationIsPersistedWithCorrectCnesAndProfile() {
        CreateTenantAdminResult result = createTenantAdminService.create(
                "Clinica Esperanca",
                "1234567",
                "Admin Esperanca",
                "admin@esperanca.local",
                "12345678901",
                "S3nha!Forte");

        List<Map<String, Object>> orgs = jdbcTemplate.queryForList(
                "SELECT cnes, display_name, fhir_meta_profile, account_active " +
                "FROM organizations WHERE tenant_id = ?",
                result.tenantId());

        assertEquals(1, orgs.size(), "exactly one organization must be persisted");
        assertEquals("1234567", orgs.get(0).get("cnes"),
                "organization cnes must match");
        assertEquals("Clinica Esperanca", orgs.get(0).get("display_name"),
                "organization display_name must match");
        assertTrue(orgs.get(0).get("fhir_meta_profile").toString().contains("BREstabelecimentoSaude"),
                "fhir_meta_profile must reference BREstabelecimentoSaude");
        assertTrue((Boolean) orgs.get(0).get("account_active"),
                "organization account_active must be true");
    }

    @Test
    void iamUserIsPersistedWithProfileTenAndLinkedToPractitioner() {
        CreateTenantAdminResult result = createTenantAdminService.create(
                "Clinica Esperanca",
                "1234567",
                "Admin Esperanca",
                "admin@esperanca.local",
                "12345678901",
                "S3nha!Forte");

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT profile, email, is_active, practitioner_id " +
                "FROM iam_users WHERE tenant_id = ?",
                result.tenantId());

        assertEquals(1, users.size(), "exactly one iam_user must be persisted");
        assertEquals(10, ((Number) users.get(0).get("profile")).intValue(),
                "iam_user profile must be 10 (admin)");
        assertEquals("admin@esperanca.local", users.get(0).get("email"),
                "iam_user email must match");
        assertTrue((Boolean) users.get(0).get("is_active"),
                "iam_user must be active");
        assertNotNull(users.get(0).get("practitioner_id"),
                "iam_user must be linked to a practitioner");
    }

    @Test
    void practitionerIsPersistedWithCpfEncryptedAndRndsProfile() {
        CreateTenantAdminResult result = createTenantAdminService.create(
                "Clinica Esperanca",
                "1234567",
                "Admin Esperanca",
                "admin@esperanca.local",
                "12345678901",
                "S3nha!Forte");

        List<Map<String, Object>> practitioners = jdbcTemplate.queryForList(
                "SELECT id, fhir_meta_profile, cpf_encrypted, encryption_key_version, " +
                "       display_name, fhir_active " +
                "FROM practitioners WHERE tenant_id = ?",
                result.tenantId());

        assertEquals(1, practitioners.size(), "exactly one practitioner must be persisted");
        assertEquals(result.adminPractitionerId(),
                java.util.UUID.fromString(practitioners.get(0).get("id").toString()),
                "practitioner id must match result adminPractitionerId");
        assertTrue(practitioners.get(0).get("fhir_meta_profile").toString()
                        .contains("BRProfissionalSaude"),
                "practitioner fhir_meta_profile must reference BRProfissionalSaude");
        assertNotNull(practitioners.get(0).get("cpf_encrypted"),
                "cpf_encrypted must be persisted");
        assertNotNull(practitioners.get(0).get("encryption_key_version"),
                "encryption_key_version must be persisted");
        assertEquals("Admin Esperanca", practitioners.get(0).get("display_name"),
                "practitioner display_name must match admin name");
    }

    @Test
    void auditEventTenantAdminCreatedIsPersisted() {
        CreateTenantAdminResult result = createTenantAdminService.create(
                "Clinica Esperanca",
                "1234567",
                "Admin Esperanca",
                "admin@esperanca.local",
                "12345678901",
                "S3nha!Forte");

        List<Map<String, Object>> auditEvents = jdbcTemplate.queryForList(
                "SELECT event_type, outcome FROM iam_audit_events WHERE tenant_id = ? AND id = ?",
                result.tenantId(), result.auditEventId());

        assertEquals(1, auditEvents.size(), "exactly one audit event must be persisted");
        assertEquals("TENANT_ADMIN_CREATED", auditEvents.get(0).get("event_type"),
                "audit event_type must be TENANT_ADMIN_CREATED");
        assertEquals("success", auditEvents.get(0).get("outcome"),
                "audit outcome must be success");
    }

    @Test
    void tenantRowIsCreatedInTenantsTable() {
        CreateTenantAdminResult result = createTenantAdminService.create(
                "Clinica Esperanca",
                "1234567",
                "Admin Esperanca",
                "admin@esperanca.local",
                "12345678901",
                "S3nha!Forte");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?",
                Integer.class,
                result.tenantId());

        assertEquals(1, count, "a tenants row must be created for the new organization");
    }
}
