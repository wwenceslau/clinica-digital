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
import org.springframework.http.HttpMethod;
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
 * T081 [US11] Contract test: GET /api/users/me/context.
 *
 * <p>TDD state: RED until {@link com.clinicadigital.gateway.api.UserContextController}
 * is implemented (T086).
 *
 * <p>Scenarios:
 * <ol>
 *   <li>Authenticated user with valid session → 200 with UserContextResponse fields.</li>
 *   <li>No session cookie → 401 (AuthenticationFilter rejects).</li>
 *   <li>Valid session but wrong tenant → 401.</li>
 * </ol>
 *
 * Refs: FR-008, FR-019, US11
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserContextContractTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";
    private static final String FHIR_IDENTIFIER_JSON =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"T081001\"}]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000000\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Dr. T081 Test\"}]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
    private static final String LOC_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC081\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_user_context_contract_test")
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
     * Scenario 1: authenticated user with active session → 200 with context fields.
     *
     * <p>TDD RED: returns 404 until UserContextController is implemented (T086).
     */
    @Test
    void authenticatedUserGetsContext() {
        // Arrange
        UUID tenantId = insertTenant("t081-ctx", "Tenant T081");
        UUID orgId = insertOrganization(tenantId, "T081001", "Org T081");
        UUID practitionerId = insertPractitioner(tenantId, "Dr. T081 Test");
        UUID locationId = insertLocation(tenantId, orgId, "Location T081");
        UUID roleId = insertPractitionerRole(orgId, practitionerId, locationId, "MD", true);
        String passwordHash = passwordService.hashPassword("S3nha@T081");
        UUID userId = insertIamUser(orgId, "context@t081.local", passwordHash, practitionerId);
        UUID sessionId = insertSession(tenantId, userId, roleId);

        // Act: GET /api/users/me/context with session + tenant
        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me/context", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        // Assert
        assertThat(response.getStatusCode())
                .as("T081: authenticated user must get 200")
                .isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body)
                .as("response must contain tenantId")
                .contains("\"tenantId\"");
        assertThat(body)
                .as("response must contain organizationName")
                .contains("\"organizationName\"");
        assertThat(body)
                .as("response must contain practitionerName")
                .contains("\"practitionerName\"");
        assertThat(body)
                .as("response must contain profileType")
                .contains("\"profileType\"");
    }

    /**
     * Scenario 2: no session cookie → 401 (AuthenticationFilter rejects unauthenticated calls).
     */
    @Test
    void noSessionReturns401() {
        // Arrange: valid tenant, no session
        UUID tenantId = insertTenant("t081-nosess", "Tenant T081 NoSess");
        insertOrganization(tenantId, "T081002", "Org T081 NoSess");

        // Act: GET /api/users/me/context without session cookie
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId.toString());
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me/context", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        // Assert
        assertThat(response.getStatusCode())
                .as("T081: missing session must return 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Scenario 3: valid session but mismatched tenant → 401.
     */
    @Test
    void validSessionWrongTenantReturns401() {
        // Arrange
        UUID tenantId = insertTenant("t081-wrongtenant", "Tenant T081 Right");
        UUID orgId = insertOrganization(tenantId, "T081003", "Org T081 Right");
        UUID practitionerId = insertPractitioner(tenantId, "Dr. Wrong Tenant");
        UUID locationId = insertLocation(tenantId, orgId, "Location T081 Right");
        UUID roleId = insertPractitionerRole(orgId, practitionerId, locationId, "RN", true);
        String passwordHash = passwordService.hashPassword("S3nha@T081");
        UUID userId = insertIamUser(orgId, "wrongtenant@t081.local", passwordHash, practitionerId);
        UUID sessionId = insertSession(tenantId, userId, roleId);

        UUID wrongTenantId = UUID.randomUUID();

        // Act: session belongs to tenantId, but request uses wrongTenantId
        HttpHeaders headers = buildAuthHeaders(wrongTenantId, sessionId);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me/context", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        // Assert: AuthenticationFilter must reject mismatched tenant
        assertThat(response.getStatusCode())
                .as("T081: mismatched tenant must return 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
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

    private UUID insertPractitionerRole(UUID orgId, UUID practitionerId,
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
        return id;
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
