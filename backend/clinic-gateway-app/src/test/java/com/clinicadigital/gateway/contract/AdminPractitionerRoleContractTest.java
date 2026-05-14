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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T159 [Phase 18] Contract tests for admin practitioner roles endpoints.
 *
 * Refs: FR-006, FR-019, FR-020
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdminPractitionerRoleContractTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000077\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Admin T159\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_admin_role_contract_test")
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

    @Test
    void adminCreatesRoleReturns201() {
        UUID tenantId = insertTenant("t159-create", "Tenant T159 Create");
        UUID orgId = insertOrganization(tenantId, "T159001", "Org T159");
        UUID locationId = insertLocation(tenantId, orgId, "Unidade T159", true);
        UUID practitionerId = insertPractitioner(tenantId, "Prof T159");
        UUID sessionId = insertAdminSession(tenantId, orgId, "admin-create@t159.local", 10);

        String requestBody = """
                {
                  "organizationId": "%s",
                  "locationId": "%s",
                  "practitionerId": "%s",
                  "roleCode": "MD",
                                    "primaryRole": true,
                                    "fhirTelecomJson": "[{\"system\":\"phone\",\"value\":\"+556199999999\"}]",
                                    "fhirAvailableTimeJson": "[{\"daysOfWeek\":[\"mon\"],\"allDay\":true}]"
                }
                """.formatted(orgId, locationId, practitionerId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/practitioner-roles",
                new HttpEntity<>(requestBody, buildAuthHeaders(tenantId, sessionId)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"roleCode\":\"MD\"");
        assertThat(response.getBody()).contains("\"fhirTelecomJson\"");
        assertThat(response.getBody()).contains("\"fhirAvailableTimeJson\"");
    }

    @Test
    void adminListsRolesReturns200() {
        UUID tenantId = insertTenant("t159-list", "Tenant T159 List");
        UUID orgId = insertOrganization(tenantId, "T159002", "Org T159 List");
        UUID locationId = insertLocation(tenantId, orgId, "Unidade Lista", true);
        UUID practitionerId = insertPractitioner(tenantId, "Prof Lista");
        UUID sessionId = insertAdminSession(tenantId, orgId, "admin-list@t159.local", 10);
        insertPractitionerRole(tenantId, orgId, locationId, practitionerId, "RN", true);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/practitioner-roles",
                HttpMethod.GET,
                new HttpEntity<>(buildAuthHeaders(tenantId, sessionId)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"roleCode\":\"RN\"");
    }

    @Test
    void adminDeactivatesRoleReturns200AndInactive() {
        UUID tenantId = insertTenant("t159-deactivate", "Tenant T159 Deactivate");
        UUID orgId = insertOrganization(tenantId, "T159003", "Org T159 Deactivate");
        UUID locationId = insertLocation(tenantId, orgId, "Unidade Deact", true);
        UUID practitionerId = insertPractitioner(tenantId, "Prof Deact");
        UUID sessionId = insertAdminSession(tenantId, orgId, "admin-deactivate@t159.local", 10);
        UUID roleId = insertPractitionerRole(tenantId, orgId, locationId, practitionerId, "MD", true);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/admin/practitioner-roles/" + roleId + "/deactivate",
                new HttpEntity<>(buildAuthHeaders(tenantId, sessionId)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"active\":false");
    }

    private HttpHeaders buildAuthHeaders(UUID tenantId, UUID sessionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId.toString());
        headers.set("Authorization", "Bearer " + sessionId);
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

    private UUID insertAdminSession(UUID tenantId, UUID orgId, String email, int profile) {
        UUID practitionerId = insertPractitioner(tenantId, "Practitioner " + email);
        UUID locationId = insertLocation(tenantId, orgId, "Location " + email, true);
        UUID roleId = insertPractitionerRole(tenantId, orgId, locationId, practitionerId, "MD", true);
        String pwHash = passwordService.hashPassword("S3nha@T159");
        UUID userId = insertIamUser(orgId, email, pwHash, practitionerId, profile);
        return insertSession(tenantId, userId, roleId);
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

    private UUID insertLocation(UUID tenantId, UUID orgId, String displayName, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO locations (id, tenant_id, organization_id, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_status, fhir_mode, account_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?,
                    '["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento"]'::jsonb,
                    '[]'::jsonb,
                    ?, ?, 'instance', ?, NOW(), NOW())
                """, id, tenantId, orgId, displayName,
                "loc-" + id, displayName, active ? "active" : "inactive", active);
        return id;
    }

    private UUID insertPractitionerRole(UUID tenantId, UUID orgId, UUID locationId,
                                        UUID practitionerId, String roleCode, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO practitioner_roles (id, tenant_id, organization_id,
                    location_id, practitioner_id, fhir_resource_id,
                    fhir_meta_profile, role_code, active, primary_role,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, false, NOW(), NOW())
                """, id, tenantId, orgId, locationId, practitionerId,
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
                INSERT INTO iam_sessions (id, tenant_id, organization_id, iam_user_id,
                    opaque_token_digest, issued_at, created_at, expires_at, active, active_practitioner_role_id)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW(), NOW() + INTERVAL '30 minutes', true, ?)
                """, id, tenantId, tenantId, userId, sha256Hex(id.toString()), activePractitionerRoleId);
        return id;
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
