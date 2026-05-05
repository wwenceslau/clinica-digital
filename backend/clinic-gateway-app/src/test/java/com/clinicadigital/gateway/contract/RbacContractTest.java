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
 * T099 [P] [US6] Contract test for RBAC group/permission management endpoints.
 *
 * <p>TDD state: RED until {@link com.clinicadigital.gateway.api.AdminGroupController}
 * and {@link com.clinicadigital.iam.application.RbacGroupService} are implemented (T102, T103).
 *
 * <p>Scenarios:
 * <ol>
 *   <li>Admin (profile 10) creates a group → 201 with groupId and name.</li>
 *   <li>Admin lists groups → 200 with group list.</li>
 *   <li>Admin lists permissions catalog → 200 with available permissions.</li>
 *   <li>Admin assigns a profile-20 user to a group → 201.</li>
 *   <li>Profile-20 user attempts to create a group → 403.</li>
 *   <li>Unauthenticated request to create group → 401.</li>
 * </ol>
 *
 * Refs: FR-005, FR-006
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class RbacContractTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000099\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Admin T099\"}]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
    private static final String LOC_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC099\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_rbac_contract_test")
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
        jdbc.execute("TRUNCATE TABLE iam_user_groups CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_group_permissions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_groups CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_sessions CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    /**
     * Scenario 1: admin (profile 10) creates a group → 201 with groupId and name.
     *
     * <p>TDD RED: returns 404 until AdminGroupController is implemented (T103).
     */
    @Test
    void adminCreatesGroupReturns201() {
        UUID tenantId = insertTenant("t099-create-group", "Tenant T099 Group");
        UUID orgId = insertOrganization(tenantId, "T099001", "Org T099");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T099");
        UUID locationId = insertLocation(tenantId, orgId, "Location T099");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        String adminPwHash = passwordService.hashPassword("S3nha@T099Admin");
        UUID adminUserId = insertIamUser(orgId, "admin@t099.local", adminPwHash, adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        String requestBody = """
                {
                  "name": "Medicos Plantonistas",
                  "description": "Grupo de medicos plantonistas"
                }
                """;

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/groups",
                new HttpEntity<>(requestBody, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T099: admin creating group must return 201")
                .isEqualTo(HttpStatus.CREATED);

        String body = response.getBody();
        assertThat(body)
                .as("T099: response must contain groupId")
                .contains("groupId");
        assertThat(body)
                .as("T099: response must contain group name")
                .contains("Medicos Plantonistas");
    }

    /**
     * Scenario 2: admin lists groups → 200 with group list.
     *
     * <p>TDD RED: returns 404 until AdminGroupController GET is implemented (T103).
     */
    @Test
    void adminListsGroupsReturns200() {
        UUID tenantId = insertTenant("t099-list-groups", "Tenant T099 List");
        UUID orgId = insertOrganization(tenantId, "T099002", "Org T099 List");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T099 List");
        UUID locationId = insertLocation(tenantId, orgId, "Location T099 List");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        String adminPwHash = passwordService.hashPassword("S3nha@T099List");
        UUID adminUserId = insertIamUser(orgId, "admin@t099list.local", adminPwHash, adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        // Pre-insert a group
        UUID groupId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO iam_groups (id, tenant_id, name, description, created_at) VALUES (?, ?, ?, ?, NOW())",
                groupId, tenantId, "Enfermeiros", "Grupo de enfermeiros");

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/groups",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T099: listing groups must return 200")
                .isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body)
                .as("T099: response must contain inserted group")
                .contains("Enfermeiros");
    }

    /**
     * Scenario 3: admin lists permissions catalog → 200 with available permissions.
     *
     * <p>TDD RED: returns 404 until AdminGroupController GET /permissions is implemented (T103).
     */
    @Test
    void adminListsPermissionsReturns200() {
        UUID tenantId = insertTenant("t099-list-perms", "Tenant T099 Perms");
        UUID orgId = insertOrganization(tenantId, "T099003", "Org T099 Perms");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T099 Perms");
        UUID locationId = insertLocation(tenantId, orgId, "Location T099 Perms");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        String adminPwHash = passwordService.hashPassword("S3nha@T099Perms");
        UUID adminUserId = insertIamUser(orgId, "admin@t099perms.local", adminPwHash, adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/permissions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T099: listing permissions catalog must return 200")
                .isEqualTo(HttpStatus.OK);
    }

    /**
     * Scenario 4: admin assigns a profile-20 user to a group → 201.
     *
     * <p>TDD RED: returns 404 until AdminGroupController POST /groups/{id}/members is implemented (T103).
     */
    @Test
    void adminAssignsUserToGroupReturns201() {
        UUID tenantId = insertTenant("t099-assign-user", "Tenant T099 Assign");
        UUID orgId = insertOrganization(tenantId, "T099004", "Org T099 Assign");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T099 Assign");
        UUID locationId = insertLocation(tenantId, orgId, "Location T099 Assign");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        String adminPwHash = passwordService.hashPassword("S3nha@T099Assign");
        UUID adminUserId = insertIamUser(orgId, "admin@t099assign.local", adminPwHash, adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        // Insert group
        UUID groupId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO iam_groups (id, tenant_id, name, created_at) VALUES (?, ?, ?, NOW())",
                groupId, tenantId, "Fisioterapeutas");

        // Insert profile-20 user
        UUID userPractitionerId = insertPractitioner(tenantId, "User T099");
        String userPwHash = passwordService.hashPassword("S3nha@User099");
        UUID userId = insertIamUser(orgId, "user@t099assign.local", userPwHash, userPractitionerId, 20);

        String requestBody = """
                {
                  "userId": "%s"
                }
                """.formatted(userId);

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/groups/" + groupId + "/members",
                new HttpEntity<>(requestBody, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T099: assigning user to group must return 201")
                .isEqualTo(HttpStatus.CREATED);
    }

    /**
     * Scenario 5: profile-20 user attempts to create a group → 403.
     *
     * <p>TDD RED: returns 404 until AdminGroupController checks profile (T103).
     */
    @Test
    void profile20UserCannotCreateGroupReturns403() {
        UUID tenantId = insertTenant("t099-deny-user", "Tenant T099 Deny");
        UUID orgId = insertOrganization(tenantId, "T099005", "Org T099 Deny");
        UUID practitionerId = insertPractitioner(tenantId, "User T099 Deny");
        UUID locationId = insertLocation(tenantId, orgId, "Location T099 Deny");
        UUID roleId = insertPractitionerRole(orgId, practitionerId, locationId, "RN", true);
        String pwHash = passwordService.hashPassword("S3nha@T099Deny");
        UUID userId = insertIamUser(orgId, "user@t099deny.local", pwHash, practitionerId, 20);
        UUID sessionId = insertSession(tenantId, userId, roleId);

        String requestBody = """
                {
                  "name": "Unauthorized Group"
                }
                """;

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/groups",
                new HttpEntity<>(requestBody, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T099: profile-20 creating group must return 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Scenario 6: unauthenticated request to create group → 401.
     *
     * <p>TDD RED: returns 404 until AdminGroupController requires auth (T103).
     */
    @Test
    void unauthenticatedCreateGroupReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/groups",
                new HttpEntity<>("{\"name\":\"No Auth Group\"}", headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T099: unauthenticated request must return 401")
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
