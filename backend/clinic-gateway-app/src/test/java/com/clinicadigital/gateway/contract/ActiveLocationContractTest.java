package com.clinicadigital.gateway.contract;

import com.clinicadigital.iam.application.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T082 [US11] Contract test: POST /api/users/me/active-location.
 *
 * <p>TDD state: RED until {@link com.clinicadigital.gateway.api.UserContextController}
 * is implemented (T087).
 *
 * <p>Scenarios:
 * <ol>
 *   <li>Valid location with active PractitionerRole → 200 with updated context.</li>
 *   <li>Location not linked to practitioner's active roles → 403 OperationOutcome.</li>
 *   <li>Location from different tenant → 403 OperationOutcome.</li>
 * </ol>
 *
 * Refs: FR-018, FR-019, US11
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class ActiveLocationContractTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000000\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Dr. T082 Test\"}]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
    private static final String LOC_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC082\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_active_loc_contract_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordService passwordService;

    @BeforeEach
    void cleanTestData() {
        jdbc.execute("TRUNCATE TABLE iam_sessions CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    /**
     * Scenario 1: practitioner has active PractitionerRole for the given location →
     * POST returns 200 with updated context including locationId and locationName.
     *
     * <p>TDD RED: returns 404 until UserContextController is implemented (T087).
     */
    @Test
    void validLocationWithActiveRoleReturns200WithUpdatedContext() {
        // Arrange
        UUID tenantId = insertTenant("t082-valid", "Tenant T082 Valid");
        UUID orgId = insertOrganization(tenantId, "T082001", "Org T082 Valid");
        UUID practitionerId = insertPractitioner(tenantId, "Dr. T082 Valid");
        UUID locationId = insertLocation(tenantId, orgId, "Unidade T082 Valid");
        insertPractitionerRole(orgId, practitionerId, locationId, "MD", true);
        String passwordHash = passwordService.hashPassword("S3nha@T082");
        UUID userId = insertIamUser(orgId, "active@t082.local", passwordHash, practitionerId);
        UUID sessionId = insertSession(tenantId, userId, null);

        // Act
        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        String body = "{\"locationId\":\"" + locationId + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users/me/active-location",
                new HttpEntity<>(body, headers), String.class);

        // Assert
        assertThat(response.getStatusCode())
                .as("T082: valid location selection must return 200")
                .isEqualTo(HttpStatus.OK);

        String responseBody = response.getBody();
        assertThat(responseBody)
                .as("response must contain locationId")
                .contains("\"locationId\"");
        assertThat(responseBody)
                .as("response must contain locationName")
                .contains("\"locationName\"");
        assertThat(responseBody)
                .as("response must include the selected location's UUID")
                .contains(locationId.toString());
    }

    /**
     * Scenario 2: practitioner has NO active PractitionerRole for the given location →
     * POST returns 403 with OperationOutcome.
     *
     * <p>TDD RED: returns 404 until UserContextController is implemented (T087).
     */
    @Test
    void locationNotLinkedToPractitionerReturns403() {
        // Arrange
        UUID tenantId = insertTenant("t082-noperm", "Tenant T082 NoPerm");
        UUID orgId = insertOrganization(tenantId, "T082002", "Org T082 NoPerm");
        UUID practitionerId = insertPractitioner(tenantId, "Dr. T082 NoPerm");
        // Note: NO practitioner_role for this location
        UUID unknownLocationId = UUID.randomUUID();
        String passwordHash = passwordService.hashPassword("S3nha@T082");
        UUID userId = insertIamUser(orgId, "noperm@t082.local", passwordHash, practitionerId);
        UUID sessionId = insertSession(tenantId, userId, null);

        // Act
        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        String body = "{\"locationId\":\"" + unknownLocationId + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users/me/active-location",
                new HttpEntity<>(body, headers), String.class);

        // Assert
        assertThat(response.getStatusCode())
                .as("T082: location without active role must return 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .as("403 response must contain FHIR OperationOutcome issue array")
                .contains("issue");
    }

    /**
     * Scenario 3: location belongs to a different tenant → 403.
     *
     * <p>TDD RED: returns 404 until UserContextController is implemented (T087).
     */
    @Test
    void locationFromDifferentTenantReturns403() {
        // Arrange: two tenants, location belongs to tenant2
        UUID tenantId = insertTenant("t082-tenant1", "Tenant T082 A");
        UUID orgId = insertOrganization(tenantId, "T082003", "Org T082 A");
        UUID practitionerId = insertPractitioner(tenantId, "Dr. T082 Tenant A");
        String passwordHash = passwordService.hashPassword("S3nha@T082");
        UUID userId = insertIamUser(orgId, "tenant1@t082.local", passwordHash, practitionerId);
        UUID sessionId = insertSession(tenantId, userId, null);

        UUID tenant2Id = insertTenant("t082-tenant2", "Tenant T082 B");
        UUID org2Id = insertOrganization(tenant2Id, "T082004", "Org T082 B");
        UUID otherTenantLocationId = insertLocation(tenant2Id, org2Id, "Location Tenant B");

        // Act: try to select location from tenant2 while authenticated as tenant1
        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        String body = "{\"locationId\":\"" + otherTenantLocationId + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users/me/active-location",
                new HttpEntity<>(body, headers), String.class);

        // Assert: cross-tenant location selection must be blocked
        assertThat(response.getStatusCode())
                .as("T082: cross-tenant location must return 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- Helpers ----

    private HttpHeaders buildAuthHeaders(UUID tenantId, UUID sessionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId.toString());
        headers.set("Authorization", "Bearer " + sessionId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private UUID insertTenant(String slug, String legalName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tenants (id, slug, legal_name, status, plan_tier,
                    quota_requests_per_minute, quota_concurrency, quota_storage_mb,
                    created_at, updated_at)
                VALUES (?, ?, ?, 'active', 'standard', 120, 10, 1024, NOW(), NOW())
                """, id, slug, legalName);
        return id;
    }

    private UUID insertOrganization(UUID tenantId, String cnes, String displayName) {
        UUID id = tenantId; // ck_organizations_tenant_is_self
        String identifier = "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"" + cnes + "\"}]";
        jdbc.update("""
                INSERT INTO organizations (id, tenant_id, cnes, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_active, account_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, true, true, NOW(), NOW())
                """, id, tenantId, cnes, displayName,
                "org-" + id, FHIR_ORG_PROFILE, identifier, displayName);
        return id;
    }

    private UUID insertPractitioner(UUID tenantId, String displayName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO practitioners (id, tenant_id, fhir_resource_id,
                    fhir_meta_profile, fhir_identifier_json, fhir_name_json,
                    display_name, cpf_encrypted, encryption_key_version,
                    fhir_active, account_active, created_at, updated_at)
                VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, decode('deadbeef', 'hex'), 'v1', true, true, NOW(), NOW())
                """, id, tenantId, "pr-" + id,
                FHIR_PRACTITIONER_PROFILE,
                FHIR_PRACTITIONER_IDENTIFIER,
                FHIR_PRACTITIONER_NAME,
                displayName);
        return id;
    }

    private UUID insertLocation(UUID tenantId, UUID orgId, String displayName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO locations (id, tenant_id, organization_id, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_status, fhir_mode, account_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, 'active', 'instance', true, NOW(), NOW())
                """, id, tenantId, orgId, displayName,
                "loc-" + id, LOC_PROFILE, LOC_IDENTIFIER, displayName);
        return id;
    }

    private void insertPractitionerRole(UUID orgId, UUID practitionerId,
                                        UUID locationId, String roleCode, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO practitioner_roles (id, tenant_id, organization_id,
                    location_id, practitioner_id, fhir_resource_id,
                    fhir_meta_profile, role_code, active, primary_role,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, false, NOW(), NOW())
                """, id, orgId, orgId, locationId, practitionerId,
                "role-" + id, FHIR_ROLE_PROFILE, roleCode, active);
    }

    private UUID insertIamUser(UUID orgId, String email, String passwordHash,
                               UUID practitionerId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO iam_users (id, tenant_id, username, email,
                    password_hash, password_algo, account_active, profile,
                    practitioner_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'bcrypt', true, 20, ?, NOW(), NOW())
                """, id, orgId, email, email, passwordHash, practitionerId);
        return id;
    }

    private UUID insertSession(UUID tenantId, UUID userId, UUID activePractitionerRoleId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO iam_sessions (id, tenant_id, iam_user_id, issued_at, created_at,
                    expires_at, active, active_practitioner_role_id)
                VALUES (?, ?, ?, NOW(), NOW(), NOW() + INTERVAL '30 minutes', true, ?)
                """, id, tenantId, userId, activePractitionerRoleId);
        return id;
    }
}
