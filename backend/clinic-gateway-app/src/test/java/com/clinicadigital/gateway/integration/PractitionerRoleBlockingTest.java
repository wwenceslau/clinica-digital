package com.clinicadigital.gateway.integration;

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
 * T083 [US11] Integration test: PractitionerRole blocking on active-location endpoint.
 *
 * <p>TDD state: RED until {@link com.clinicadigital.gateway.api.UserContextController}
 * and {@link com.clinicadigital.iam.application.UserContextService} are implemented (T085-T087).
 *
 * <p>Scenarios:
 * <ol>
 *   <li>Practitioner with INACTIVE role for a location tries to set it as active → 403.</li>
 *   <li>Practitioner with unknown/non-existent location → 403.</li>
 * </ol>
 *
 * Refs: FR-018, FR-019, US11
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class PractitionerRoleBlockingTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000000\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Dr. T083 Blocking\"}]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
    private static final String LOC_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC083\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_practitioner_role_blocking_test")
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
     * Scenario 1: practitioner has an INACTIVE PractitionerRole for the location.
     * The business rule requires the role to be active; inactive roles must be blocked.
     *
     * <p>TDD RED: returns 404 until UserContextController is implemented (T087).
     */
    @Test
    void inactiveRoleForLocationIsBlocked() {
        // Arrange: practitioner has an INACTIVE role for locationId
        UUID tenantId = insertTenant("t083-inactive", "Tenant T083 Inactive");
        UUID orgId = insertOrganization(tenantId, "T083001", "Org T083 Inactive");
        UUID practitionerId = insertPractitioner(tenantId, "Dr. T083 Inactive Role");
        UUID locationId = insertLocation(tenantId, orgId, "Location T083 Inactive");
        insertPractitionerRole(orgId, practitionerId, locationId, "MD", false); // inactive!
        String passwordHash = passwordService.hashPassword("S3nha@T083");
        UUID userId = insertIamUser(orgId, "inactive@t083.local", passwordHash, practitionerId);
        UUID sessionId = insertSession(tenantId, userId, null);

        // Act
        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        String body = "{\"locationId\":\"" + locationId + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users/me/active-location",
                new HttpEntity<>(body, headers), String.class);

        // Assert: inactive role must be rejected
        assertThat(response.getStatusCode())
                .as("T083: inactive PractitionerRole must return 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .as("response must contain FHIR OperationOutcome issue details")
                .contains("issue");
    }

    /**
     * Scenario 2: practitioner sends a locationId that doesn't exist in the DB at all.
     * Must return 403 (not 404) to avoid leaking resource existence.
     *
     * <p>TDD RED: returns 404 until UserContextController is implemented (T087).
     */
    @Test
    void unknownLocationIsBlocked() {
        // Arrange: practitioner has no role for this UUID
        UUID tenantId = insertTenant("t083-unknown", "Tenant T083 Unknown");
        UUID orgId = insertOrganization(tenantId, "T083002", "Org T083 Unknown");
        UUID practitionerId = insertPractitioner(tenantId, "Dr. T083 Unknown Loc");
        String passwordHash = passwordService.hashPassword("S3nha@T083");
        UUID userId = insertIamUser(orgId, "unknown@t083.local", passwordHash, practitionerId);
        UUID sessionId = insertSession(tenantId, userId, null);

        UUID nonExistentLocationId = UUID.randomUUID();

        // Act
        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        String body = "{\"locationId\":\"" + nonExistentLocationId + "\"}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users/me/active-location",
                new HttpEntity<>(body, headers), String.class);

        // Assert: unknown location must return 403 (not 404)
        assertThat(response.getStatusCode())
                .as("T083: unknown locationId must return 403 to prevent resource discovery")
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .as("response must contain FHIR OperationOutcome issue details")
                .contains("issue");
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
