package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.application.AuthChallengeService;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T051 — Integration test: challenge resolution edge cases.
 *
 * <p>Scenarios:
 * <ul>
 *   <li>Expired challenge token → 401 Unauthorized.</li>
 *   <li>Organization not listed in challenge → 401 Unauthorized.</li>
 *   <li>Valid challenge + allowed org → 200 with sessionId and cookie.</li>
 * </ul>
 *
 * Refs: FR-007, US4; specs/004-institution-iam-auth-integration/plan.md
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthChallengeIntegrationTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"11111111111\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Challenge Practitioner\"}]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_challenge_integration_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordService passwordService;

    @Autowired
    AuthChallengeService authChallengeService;

    @BeforeEach
    void cleanTestData() {
        // TRUNCATE ignores FK constraints and is faster
        jdbc.execute("TRUNCATE TABLE iam_sessions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_auth_challenges CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    // ---- Scenario: expired challenge ----

    @Test
    void expiredChallengeMustReturn401() {
        // Arrange: create a valid user, then insert an expired challenge directly
        UUID tenantId = insertTenant("challenge-expired", "Challenge Expired Tenant");
        UUID orgId = insertOrganization(tenantId, "CH0001", "Challenge Expired Org");
        UUID practitionerId = insertPractitioner(tenantId, "expired-pr");
        UUID userId = insertIamUser(orgId, "ch-expired@test.local",
                passwordService.hashPassword("S3nha@Forte"), practitionerId);

        UUID challengeId = UUID.randomUUID();
        String digest = sha256Hex("expired-token");
        String optionsJson = "[{\"organizationId\":\"" + orgId + "\",\"displayName\":\"Org\",\"cnes\":\"CH0001\"}]";
        Instant alreadyExpired = Instant.now().minusSeconds(60);

        jdbc.update("""
                INSERT INTO iam_auth_challenges
                    (id, iam_user_id, challenge_token_digest, organization_options_json, expires_at, created_at)
                VALUES (?, ?, ?, ?::jsonb, ?, NOW())
                                """, challengeId, userId, digest, optionsJson, Timestamp.from(alreadyExpired));

        // Act
        ResponseEntity<String> response = postSelectOrg("expired-token", orgId);

        // Assert
        assertThat(response.getStatusCode())
                .as("expired challenge must return 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- Scenario: org not in challenge allowed list ----

    @Test
    void orgNotInChallengeMustReturn401() {
        // Arrange: set up real user + org, create challenge via service
        UUID tenantId = insertTenant("challenge-notallowed", "Challenge Tenant");
        UUID orgId = insertOrganization(tenantId, "CH0002", "Challenge Org");
        UUID practitionerId = insertPractitioner(tenantId, "challenge-pr");
        UUID userId = insertIamUser(orgId, "ch-notallowed@test.local",
                passwordService.hashPassword("S3nha@Forte"), practitionerId);

        // Create challenge with orgId as allowed org
        List<AuthChallengeService.OrganizationOption> options = List.of(
                new AuthChallengeService.OrganizationOption(orgId, "Challenge Org", "CH0002"));
        String challengeToken = authChallengeService.createChallenge(userId, options);

        // Act: send a DIFFERENT organizationId (not in challenge)
        UUID wrongOrgId = UUID.randomUUID();
        ResponseEntity<String> response = postSelectOrg(challengeToken, wrongOrgId);

        // Assert
        assertThat(response.getStatusCode())
                .as("organization not in challenge must return 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- Scenario: valid challenge + allowed org → success ----

    @Test
    void validChallengeWithAllowedOrgReturns200() {
        // Arrange
        UUID tenantId = insertTenant("challenge-valid", "Valid Challenge Tenant");
        UUID orgId = insertOrganization(tenantId, "CH0003", "Valid Challenge Org");
        UUID practitionerId = insertPractitioner(tenantId, "valid-challenge-pr");
        UUID locationId = insertLocation(tenantId, orgId, "Valid Location");
        insertPractitionerRole(orgId, practitionerId, locationId, "MD", true);
        UUID userId = insertIamUser(orgId, "ch-valid@test.local",
                passwordService.hashPassword("S3nha@Forte"), practitionerId);

        List<AuthChallengeService.OrganizationOption> options = List.of(
                new AuthChallengeService.OrganizationOption(orgId, "Valid Challenge Org", "CH0003"));
        String challengeToken = authChallengeService.createChallenge(userId, options);

        // Act
        ResponseEntity<String> response = postSelectOrg(challengeToken, orgId);

        // Assert
        assertThat(response.getStatusCode())
                .as("valid challenge with allowed org must return 200")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("response must contain sessionId")
                .contains("\"sessionId\"");
        assertThat(response.getHeaders().get("Set-Cookie"))
                .as("session cookie must be set")
                .isNotNull()
                .anyMatch(c -> c.contains("cd_session"));
    }

    // ---- Helpers ----

    private ResponseEntity<String> postSelectOrg(String challengeToken, UUID orgId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"challengeToken\":\"%s\",\"organizationId\":\"%s\"}",
                challengeToken, orgId);
        return restTemplate.postForEntity(
                "/api/auth/select-organization",
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
                FHIR_PRACTITIONER_PROFILE, FHIR_PRACTITIONER_IDENTIFIER,
                FHIR_PRACTITIONER_NAME, displayName);
        return id;
    }

    private UUID insertLocation(UUID tenantId, UUID orgId, String displayName) {
        UUID id = UUID.randomUUID();
        String locProfile = "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
        String locIdentifier = "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC002\"}]";
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

    /** Minimal SHA-256 hex for inserting a pre-expired challenge with known digest. */
    private static String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
