package com.clinicadigital.gateway.integration;

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
 * T100 [P] [US6] Integration test for RBAC enforcement.
 *
 * <p>Validates that:
 * <ol>
 *   <li>A user assigned to a group with a permission can be confirmed as having that permission
 *       (via the {@code GET /api/admin/users/{userId}/permissions} endpoint).</li>
 *   <li>A user NOT assigned to any group does not have the permission.</li>
 *   <li>Admin can assign a permission to a group and list group permissions.</li>
 *   <li>RLS prevents cross-tenant group access: group from tenant A is not visible to tenant B.</li>
 * </ol>
 *
 * TDD state: RED until RbacGroupService and AdminGroupController are implemented (T102, T103).
 *
 * Refs: FR-005, FR-006
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class RbacEnforcementIntegrationTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000100\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Admin T100\"}]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
    private static final String LOC_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC100\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_rbac_enforcement_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanTestData() {
        jdbc.execute("TRUNCATE TABLE iam_user_groups CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_group_permissions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_groups CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_permissions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_sessions CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    /**
     * Scenario 1: user in group with permission → GET /api/admin/users/{userId}/permissions
     * returns the permission code.
     *
     * <p>TDD RED: returns 404 until implemented (T103).
     */
    @Test
    void userInGroupWithPermissionIsReportedAsHavingIt() {
        UUID tenantId = insertTenant("t100-enforce-a", "Tenant T100 A");
        UUID orgId = insertOrganization(tenantId, "T100001", "Org T100 A");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T100 A");
        UUID locationId = insertLocation(tenantId, orgId, "Location T100 A");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        UUID adminUserId = insertIamUser(orgId, "admin@t100a.local", adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        // Create profile-20 user
        UUID userPractitionerId = insertPractitioner(tenantId, "User T100 A");
        UUID userId = insertIamUser(orgId, "user@t100a.local", userPractitionerId, 20);

        // Create group, assign permission and user to it
        UUID groupId = insertGroup(tenantId, "Medicos A");
        UUID permId = insertPermission("appointments.read", "appointments", "read");
        assignPermissionToGroup(groupId, permId);
        assignUserToGroup(userId, groupId);

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/users/" + userId + "/permissions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T100: user with group permission must return 200")
                .isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body)
                .as("T100: response must contain the assigned permission code")
                .contains("appointments.read");
    }

    /**
     * Scenario 2: user NOT in any group → GET /api/admin/users/{userId}/permissions
     * returns empty permissions list.
     *
     * <p>TDD RED: returns 404 until implemented (T103).
     */
    @Test
    void userWithNoGroupHasEmptyPermissions() {
        UUID tenantId = insertTenant("t100-no-group", "Tenant T100 No Group");
        UUID orgId = insertOrganization(tenantId, "T100002", "Org T100 No Group");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T100 NoGroup");
        UUID locationId = insertLocation(tenantId, orgId, "Location T100 NoGroup");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        UUID adminUserId = insertIamUser(orgId, "admin@t100nogroup.local", adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        UUID userPractitionerId = insertPractitioner(tenantId, "User T100 NoGroup");
        UUID userId = insertIamUser(orgId, "user@t100nogroup.local", userPractitionerId, 20);

        // No group assignment

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/users/" + userId + "/permissions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("T100: user without group must return 200 with empty permissions")
                .isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body)
                .as("T100: permissions list must be empty array")
                .contains("[]").doesNotContain("appointments");
    }

    /**
     * Scenario 3: admin assigns permission to group → GET /api/admin/groups/{id}/permissions
     * lists the assigned permission.
     *
     * <p>TDD RED: returns 404 until implemented (T103).
     */
    @Test
    void adminAssignsPermissionToGroupAndCanListIt() {
        UUID tenantId = insertTenant("t100-perm-assign", "Tenant T100 Perm");
        UUID orgId = insertOrganization(tenantId, "T100003", "Org T100 Perm");
        UUID adminPractitionerId = insertPractitioner(tenantId, "Admin T100 Perm");
        UUID locationId = insertLocation(tenantId, orgId, "Location T100 Perm");
        UUID adminRoleId = insertPractitionerRole(orgId, adminPractitionerId, locationId, "MD", true);
        UUID adminUserId = insertIamUser(orgId, "admin@t100perm.local", adminPractitionerId, 10);
        UUID sessionId = insertSession(tenantId, adminUserId, adminRoleId);

        UUID groupId = insertGroup(tenantId, "Especialistas");
        UUID permId = insertPermission("billing.write", "billing", "write");

        // Assign permission via API
        String assignBody = """
                {
                  "permissionId": "%s"
                }
                """.formatted(permId);

        HttpHeaders headers = buildAuthHeaders(tenantId, sessionId);
        ResponseEntity<String> assignResponse = restTemplate.postForEntity(
                "/api/admin/groups/" + groupId + "/permissions",
                new HttpEntity<>(assignBody, headers),
                String.class);

        assertThat(assignResponse.getStatusCode())
                .as("T100: assigning permission to group must return 201")
                .isEqualTo(HttpStatus.CREATED);

        // List group permissions
        ResponseEntity<String> listResponse = restTemplate.exchange(
                "/api/admin/groups/" + groupId + "/permissions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(listResponse.getStatusCode())
                .as("T100: listing group permissions must return 200")
                .isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody())
                .as("T100: listed permissions must include assigned permission")
                .contains("billing.write");
    }

    /**
     * Scenario 4: RLS prevents cross-tenant group access.
     *
     * Group from tenant A must NOT be visible when listing groups for tenant B.
     */
    @Test
    void rlsBlocksCrossTenantGroupAccess() {
        UUID tenantA = insertTenant("t100-rls-a", "Tenant T100 RLS A");
        UUID orgA = insertOrganization(tenantA, "T100004A", "Org A");
        UUID adminAPractitionerId = insertPractitioner(tenantA, "Admin T100 A");
        UUID locA = insertLocation(tenantA, orgA, "Location A");
        UUID roleA = insertPractitionerRole(orgA, adminAPractitionerId, locA, "MD", true);
        UUID adminAId = insertIamUser(orgA, "admin@t100rlsa.local", adminAPractitionerId, 10);
        UUID sessionA = insertSession(tenantA, adminAId, roleA);

        UUID tenantB = insertTenant("t100-rls-b", "Tenant T100 RLS B");
        UUID orgB = insertOrganization(tenantB, "T100004B", "Org B");
        UUID adminBPractitionerId = insertPractitioner(tenantB, "Admin T100 B");
        UUID locB = insertLocation(tenantB, orgB, "Location B");
        UUID roleB = insertPractitionerRole(orgB, adminBPractitionerId, locB, "MD", true);
        UUID adminBId = insertIamUser(orgB, "admin@t100rlsb.local", adminBPractitionerId, 10);
        UUID sessionB = insertSession(tenantB, adminBId, roleB);

        // Group belongs to tenant A
        insertGroup(tenantA, "Grupo Exclusivo A");

        // Tenant B must not see tenant A's group
        HttpHeaders headersB = buildAuthHeaders(tenantB, sessionB);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/groups",
                HttpMethod.GET,
                new HttpEntity<>(headersB),
                String.class);

        assertThat(response.getStatusCode())
                .as("T100 RLS: listing groups for tenant B must return 200")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("T100 RLS: tenant B must not see tenant A's group")
                .doesNotContain("Grupo Exclusivo A");
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
                    display_name, cpf_encrypted, encryption_key_version,
                    fhir_active, account_active, created_at, updated_at)
                VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, decode('deadbeef', 'hex'), 'v1', true, true, NOW(), NOW())
                """, id, tenantId, "pr-" + id,
                FHIR_PRACTITIONER_PROFILE, FHIR_PRACTITIONER_IDENTIFIER, FHIR_PRACTITIONER_NAME, displayName);
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

    private UUID insertIamUser(UUID orgId, String email, UUID practitionerId, int profile) {
        UUID id = UUID.randomUUID();
        String pwHash = "$2a$10$dummyhashfortest";
        jdbc.update("""
                INSERT INTO iam_users (id, tenant_id, username, email,
                    password_hash, password_algo, account_active, profile,
                    practitioner_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'bcrypt', true, ?, ?, NOW(), NOW())
                """, id, orgId, email, email, pwHash, profile, practitionerId);
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

    private UUID insertGroup(UUID tenantId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO iam_groups (id, tenant_id, name, created_at) VALUES (?, ?, ?, NOW())",
                id, tenantId, name);
        return id;
    }

    private UUID insertPermission(String code, String resource, String action) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO iam_permissions (id, code, resource, action) VALUES (?, ?, ?, ?)",
                id, code, resource, action);
        return id;
    }

    private void assignPermissionToGroup(UUID groupId, UUID permId) {
        jdbc.update("INSERT INTO iam_group_permissions (group_id, permission_id) VALUES (?, ?)",
                groupId, permId);
    }

    private void assignUserToGroup(UUID userId, UUID groupId) {
        jdbc.update("INSERT INTO iam_user_groups (iam_user_id, group_id, assigned_at) VALUES (?, ?, NOW())",
                userId, groupId);
    }
}
