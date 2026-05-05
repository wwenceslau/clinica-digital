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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T050 — Integration test: POST /api/auth/login multi-org modes.
 *
 * <p>Scenarios:
 * <ul>
 *   <li>Single-org mode: user with exactly 1 active practitioner_role → 200
 *       with {@code mode:"single"} and sessionId cookie.</li>
 *   <li>Multiple-org mode: user with 2 active practitioner_roles in different
 *       organizations → 200 with {@code mode:"multiple"} and challengeToken.</li>
 *   <li>No-org mode: user exists but has no active practitioner_roles → 401
 *       (treated as invalid credentials per spec FR-019).</li>
 * </ul>
 *
 * <p>Test data is inserted via JdbcTemplate (superuser, bypasses RLS).
 * The application service calls {@code applyLoginContext()} which activates
 * the V204 crosslogin RLS policies for cross-tenant lookups.
 *
 * Refs: FR-004, FR-019, US4; specs/004-institution-iam-auth-integration/plan.md
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class LoginMultiOrgIntegrationTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_IDENTIFIER_JSON =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"CNES001\"}]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000000\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Test Practitioner\"}]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_multiorg_integration_test")
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
        // TRUNCATE ignores FK constraints and is faster
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    // ---- Scenario: single-org mode ----

    @Test
    void loginSingleOrgReturns200WithModeSingleAndSessionCookie() {
        // Arrange
        UUID tenantId = insertTenant("test-single", "Tenant Single");
        UUID orgId = insertOrganization(tenantId, "TST0001", "Org Single");
        UUID practitionerId = insertPractitioner(tenantId, "single-practitioner");
        UUID locationId = insertLocation(tenantId, orgId, "Location Single");
        insertPractitionerRole(orgId, practitionerId, locationId, "MD", true);
        String passwordHash = passwordService.hashPassword("S3nha@Forte");
        insertIamUser(orgId, "single@test.local", passwordHash, practitionerId);

        // Act
        ResponseEntity<String> response = postLogin("single@test.local", "S3nha@Forte");

        // Assert
        assertThat(response.getStatusCode())
                .as("single-org login must return 200")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("response must contain mode:single")
                .contains("\"mode\":\"single\"");
        assertThat(response.getBody())
                .as("response must contain sessionId")
                .contains("\"sessionId\"");
        assertThat(response.getHeaders().get("Set-Cookie"))
                .as("session cookie must be set")
                .isNotNull()
                .anyMatch(c -> c.contains("cd_session"));
    }

    // ---- Scenario: multiple-org mode ----

    @Test
    void loginMultipleOrgsReturns200WithModeMultipleAndChallengeToken() {
        // Arrange: 2 separate tenants (each org IS its own tenant per ck_organizations_tenant_is_self)
        UUID tenantId1 = insertTenant("test-multi-a", "Tenant Multi A");
        UUID tenantId2 = insertTenant("test-multi-b", "Tenant Multi B");
        UUID orgId1 = insertOrganization(tenantId1, "TST0002", "Org Multi A");
        UUID orgId2 = insertOrganization(tenantId2, "TST0003", "Org Multi B");
        UUID practitionerId = insertPractitioner(tenantId1, "multi-practitioner");
        UUID locId1 = insertLocation(tenantId1, orgId1, "Location A");
        UUID locId2 = insertLocation(tenantId2, orgId2, "Location B");
        insertPractitionerRole(orgId1, practitionerId, locId1, "MD", true);
        insertPractitionerRole(orgId2, practitionerId, locId2, "RN", true);
        String passwordHash = passwordService.hashPassword("S3nha@Forte");
        insertIamUser(orgId1, "multi@test.local", passwordHash, practitionerId);

        // Act
        ResponseEntity<String> response = postLogin("multi@test.local", "S3nha@Forte");

        // Assert
        assertThat(response.getStatusCode())
                .as("multi-org login must return 200")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("response must contain mode:multiple")
                .contains("\"mode\":\"multiple\"");
        assertThat(response.getBody())
                .as("response must contain challengeToken")
                .contains("\"challengeToken\"");
        assertThat(response.getBody())
                .as("response must contain organizations list")
                .contains("\"organizations\"");
    }

    // ---- Scenario: no-org mode (credentials valid but no active roles) ----

    @Test
    void loginWithNoActiveOrgsReturns401() {
        // Arrange: user exists but has NO practitioner_role record
        UUID tenantId = insertTenant("test-noorg", "Tenant NoOrg");
        UUID orgId = insertOrganization(tenantId, "TST0004", "Org NoOrg");
        // No practitioner or role inserted
        String passwordHash = passwordService.hashPassword("S3nha@Forte");
        // Insert user with null practitioner_id → loginByEmail will throw InvalidCredentialsException
        insertIamUser(orgId, "noorg@test.local", passwordHash, null);

        // Act
        ResponseEntity<String> response = postLogin("noorg@test.local", "S3nha@Forte");

        // Assert
        assertThat(response.getStatusCode())
                .as("login with no active orgs must return 401 (FR-019)")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- Helpers ----

    private ResponseEntity<String> postLogin(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        return restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(body, headers),
                String.class);
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
        // ck_organizations_tenant_is_self: organization.id must equal organization.tenant_id
        UUID id = tenantId;
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
                                        display_name, cpf_encrypted, encryption_key_version, fhir_active, account_active, created_at, updated_at)
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
        String locProfile = "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
        String locIdentifier = "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC001\"}]";
        jdbc.update("""
                INSERT INTO locations (id, tenant_id, organization_id, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_status, fhir_mode, account_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, 'active', 'instance', true, NOW(), NOW())
                """, id, tenantId, orgId, displayName,
                "loc-" + id, locProfile, locIdentifier, displayName);
        return id;
    }

    private void insertPractitionerRole(UUID orgId, UUID practitionerId,
                                         UUID locationId, String roleCode,
                                         boolean active) {
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
}
