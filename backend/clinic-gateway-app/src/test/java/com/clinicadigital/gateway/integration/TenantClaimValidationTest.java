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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T073 [US5] — Backend integration test: tenant claim validation.
 *
 * <p>Verifies FR-007: every authenticated request to {@code /api/**} (except
 * {@code /api/auth/**} and {@code /api/public/**}) must carry a valid session
 * that matches the declared {@code X-Tenant-ID} header.
 *
 * <p>Scenarios covered:
 * <ol>
 *   <li>Authenticated request with valid session + matching tenant → not 401/403.</li>
 *   <li>Request to protected endpoint without session → 401.</li>
 *   <li>Request with mismatched session/tenant → 401.</li>
 * </ol>
 *
 * Refs: FR-007, US5; specs/004-institution-iam-auth-integration/tasks.md T073
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class TenantClaimValidationTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_IDENTIFIER_JSON =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"CNES073\"}]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"07300000000\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"T073 Practitioner\"}]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_tenant_claim_test")
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
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    // -----------------------------------------------------------------------
    // Scenario 1: valid session + matching tenant → allowed through
    // -----------------------------------------------------------------------

    /**
     * FR-007: a request carrying a valid session cookie and matching X-Tenant-ID
     * header must NOT be rejected with 401 or 403.
     */
    @Test
    void authenticatedRequestWithValidSessionAndMatchingTenantMustNotReturn401Or403() {
        UUID tenantId = insertTenant("t073-valid", "T073 Valid");
        UUID orgId = insertOrganization(tenantId, "T073001", "T073 Org");
        UUID practitionerId = insertPractitioner(tenantId, "T073 Practitioner");
        UUID locationId = insertLocation(tenantId, orgId, "T073 Location");
        insertPractitionerRole(orgId, practitionerId, locationId, "MD", true);
        String hash = passwordService.hashPassword("T073@Forte");
        insertIamUser(orgId, "t073@test.local", hash, practitionerId);

        // Log in to acquire a real session cookie
        ResponseEntity<String> loginResponse = postLogin("t073@test.local", "T073@Forte");
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String sessionCookie = extractSessionCookie(loginResponse);
        assertThat(sessionCookie).as("login must return a session cookie").isNotNull();

        // Hit any protected /api/** endpoint that is NOT /api/auth/**
        // /api/auth/logout is itself a public auth endpoint so use a generic endpoint
        // that would otherwise return 404 (endpoint doesn't exist) but NOT 401/403
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId.toString());
        headers.set("Cookie", sessionCookie);
        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                "/api/probe-t073",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        // 404 means the filter allowed it through (no endpoint); 401/403 means auth failed
        assertThat(protectedResponse.getStatusCode())
                .as("FR-007: valid session+tenant must not be rejected with 401 or 403")
                .isNotEqualTo(HttpStatus.UNAUTHORIZED)
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    // -----------------------------------------------------------------------
    // Scenario 2: protected endpoint without session → 401
    // -----------------------------------------------------------------------

    /**
     * FR-007: an unauthenticated request to a protected /api/** route must return 401.
     */
    @Test
    void protectedEndpointWithoutSessionMustReturn401() {
        UUID tenantId = UUID.randomUUID();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId.toString());
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/probe-t073-unauthed",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("FR-007: request without session must be rejected with 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // -----------------------------------------------------------------------
    // Scenario 3: mismatched session/tenant → 401
    // -----------------------------------------------------------------------

    /**
     * FR-007: a request with a valid session but a different X-Tenant-ID must return 401.
     */
    @Test
    void requestWithMismatchedSessionAndTenantMustReturn401() {
        UUID tenantId = insertTenant("t073-mismatch", "T073 Mismatch");
        UUID orgId = insertOrganization(tenantId, "T073002", "T073 Mismatch Org");
        UUID practitionerId = insertPractitioner(tenantId, "T073 Mismatch Practitioner");
        UUID locationId = insertLocation(tenantId, orgId, "T073 Mismatch Location");
        insertPractitionerRole(orgId, practitionerId, locationId, "MD", true);
        String hash = passwordService.hashPassword("T073@Mismatch");
        insertIamUser(orgId, "t073mismatch@test.local", hash, practitionerId);

        ResponseEntity<String> loginResponse = postLogin("t073mismatch@test.local", "T073@Mismatch");
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String sessionCookie = extractSessionCookie(loginResponse);
        assertThat(sessionCookie).as("login must return a session cookie").isNotNull();

        // Use a different (unrelated) tenant ID in the header
        UUID differentTenantId = UUID.randomUUID();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", differentTenantId.toString());
        headers.set("Cookie", sessionCookie);
        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                "/api/probe-t073-mismatch",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(protectedResponse.getStatusCode())
                .as("FR-007: session/tenant mismatch must return 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // -----------------------------------------------------------------------
    // Helper: POST /api/auth/login
    // -----------------------------------------------------------------------

    private ResponseEntity<String> postLogin(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        return restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(body, headers),
                String.class);
    }

    private String extractSessionCookie(ResponseEntity<String> response) {
        List<String> setCookie = response.getHeaders().get("Set-Cookie");
        if (setCookie == null) return null;
        return setCookie.stream()
                .filter(c -> c.startsWith("cd_session="))
                .map(c -> c.split(";")[0])
                .findFirst()
                .orElse(null);
    }

    // -----------------------------------------------------------------------
    // Data helpers
    // -----------------------------------------------------------------------

    private UUID insertTenant(String slug, String legalName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tenants (id, slug, legal_name, status, plan_tier,
                    quota_requests_per_minute, quota_concurrency, quota_storage_mb,
                    created_at, updated_at)
                VALUES (?, ?, ?, 'active', 'basic', 60, 20, 1024, NOW(), NOW())
                """, id, slug, legalName);
        return id;
    }

    private UUID insertOrganization(UUID tenantId, String cnes, String displayName) {
        UUID id = tenantId; // ck_organizations_tenant_is_self constraint
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
                VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?,
                    decode('deadbeef', 'hex'), 'v1', true, true, NOW(), NOW())
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
        String locIdentifier = "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC073\"}]";
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
