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
 * T089 [P] Contract test: POST /api/admin/users (profile 20).
 *
 * <p>TDD state: RED until {@link com.clinicadigital.gateway.api.AdminUserController}
 * and {@link com.clinicadigital.iam.application.CreateProfile20UserService} are
 * implemented (T093, T094).
 *
 * <p>Scenarios:
 * <ol>
 *   <li>Admin (profile 10) creates profile 20 user → 201 with userId, practitionerId, practitionerRoleId.</li>
 *   <li>No session → 401 (AuthenticationFilter rejects unauthenticated calls).</li>
 *   <li>Email already exists in same tenant → 409 OperationOutcome.</li>
 * </ol>
 *
 * Refs: FR-006, FR-009
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class CreateProfile20UserContractTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";
    private static final String FHIR_IDENTIFIER_JSON =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"T089001\"}]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000000\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Admin T089\"}]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
    private static final String LOC_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC089\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_create_user20_contract_test")
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
     * Scenario 1: admin (profile 10) creates profile 20 user → 201 with response fields.
     *
     * <p>TDD RED: returns 404 until AdminUserController is implemented (T093).
     */
    @Test
    void adminCreatesProfile20UserReturns201() {
        // Arrange
        UUID tenantId = insertTenant("t089-admin", "Tenant T089 Admin");
        UUID orgId = insertOrganization(tenantId, "T089001", "Org T089");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T089");
        UUID locationId = insertLocation(tenantId, orgId, "Location T089");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        String passwordHash = passwordService.hashPassword("S3nha@T089Admin");
        UUID adminUserId = insertIamUser(orgId, "admin@t089.local", passwordHash, adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        String requestBody = """
                {
                  "practitioner": {
                    "displayName": "Dr. Profile20 T089",
                    "email": "profile20@t089.local",
                    "cpf": "98765432100",
                    "password": "S3nha@Profile20",
                    "identifiers": [{"system": "https://saude.gov.br/sid/cpf", "value": "98765432100"}],
                    "names": [{"use": "official", "text": "Dr. Profile20 T089"}],
                    "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude"]}
                  },
                  "locationId": "%s",
                  "roleCode": "RN"
                }
                """.formatted(locationId);

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/users",
                new HttpEntity<>(requestBody, headers),
                String.class);

        // Assert
        assertThat(response.getStatusCode())
                .as("T089: admin user creation must return 201")
                .isEqualTo(HttpStatus.CREATED);

        String body = response.getBody();
        assertThat(body)
                .as("response must contain userId")
                .contains("\"userId\"");
        assertThat(body)
                .as("response must contain practitionerId")
                .contains("\"practitionerId\"");
        assertThat(body)
                .as("response must contain practitionerRoleId")
                .contains("\"practitionerRoleId\"");
    }

    /**
     * Scenario 2: no session → 401 (AuthenticationFilter rejects unauthenticated calls).
     */
    @Test
    void noSessionReturns401() {
        UUID tenantId = insertTenant("t089-nosess", "Tenant T089 NoSess");
        insertOrganization(tenantId, "T089002", "Org T089 NoSess");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = """
                {
                  "practitioner": {
                    "displayName": "Noauth User",
                    "email": "noauth@t089.local",
                    "cpf": "11122233344",
                    "password": "S3nha@NoAuth",
                    "identifiers": [{"system": "https://saude.gov.br/sid/cpf", "value": "11122233344"}],
                    "names": [{"use": "official", "text": "Noauth User"}],
                    "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude"]}
                  },
                  "locationId": "00000000-0000-0000-0000-000000000001",
                  "roleCode": "MD"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/users",
                new HttpEntity<>(requestBody, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T089: missing session must return 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Scenario 3: duplicate email within same tenant → 409 OperationOutcome.
     *
     * <p>TDD RED: returns 404 until AdminUserController + service email conflict handling
     * is implemented (T093, T095).
     */
    @Test
    void duplicateEmailInSameTenantReturns409() {
        // Arrange
        UUID tenantId = insertTenant("t089-dup", "Tenant T089 Dup");
        UUID orgId = insertOrganization(tenantId, "T089003", "Org T089 Dup");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T089 Dup");
        UUID locationId = insertLocation(tenantId, orgId, "Location T089 Dup");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        String adminPwHash = passwordService.hashPassword("S3nha@T089Dup");
        UUID adminUserId = insertIamUser(orgId, "admin@t089dup.local", adminPwHash, adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        // First: insert an existing profile 20 user with this email
        UUID existingPractitionerId = insertPractitioner(tenantId, "Existing User");
        String existingPwHash = passwordService.hashPassword("S3nha@Existing");
        insertIamUser(orgId, "duplicate@t089dup.local", existingPwHash, existingPractitionerId, 20);

        String requestBody = """
                {
                  "practitioner": {
                    "displayName": "Duplicate User",
                    "email": "duplicate@t089dup.local",
                    "cpf": "55566677788",
                    "password": "S3nha@Duplicate",
                    "identifiers": [{"system": "https://saude.gov.br/sid/cpf", "value": "55566677788"}],
                    "names": [{"use": "official", "text": "Duplicate User"}],
                    "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude"]}
                  },
                  "locationId": "%s",
                  "roleCode": "RN"
                }
                """.formatted(locationId);

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/users",
                new HttpEntity<>(requestBody, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T089: duplicate email within tenant must return 409")
                .isEqualTo(HttpStatus.CONFLICT);

        String body = response.getBody();
        assertThat(body)
                .as("T089: conflict response must be OperationOutcome")
                .contains("OperationOutcome");
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
                               UUID practitionerId, int profile) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO iam_users (id, tenant_id, username, email,
                    password_hash, password_algo, account_active, profile,
                    practitioner_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'bcrypt', true, ?, ?, NOW(), NOW())
                """, id, orgId, email, email, passwordHash, profile, practitionerId);
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
