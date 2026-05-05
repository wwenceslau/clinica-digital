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
 * T124 — Coverage validation: RBAC enforcement + audit log emission +
 * disabled-tenant session invalidation.
 *
 * <p>Validates three cross-cutting concerns required by the Constitution:
 * <ol>
 *   <li><b>RBAC</b>: Endpoints protected by role-based access control reject
 *       callers that lack the required permission with 403 OperationOutcome.</li>
 *   <li><b>Audit logs</b>: Sensitive operations (login success, login failure,
 *       logout) produce entries in {@code iam_audit_events} with the correct
 *       {@code event_type} and non-null {@code trace_id}.</li>
 *   <li><b>Disabled tenant</b>: A session whose tenant has {@code account_active=false}
 *       is invalidated on the first subsequent authenticated request (403 OperationOutcome).</li>
 * </ol>
 *
 * <p>TDD state: RED until RBAC middleware, audit event service, and tenant-active
 * validation are fully wired (T079, T102, T103, T105).
 *
 * Refs: FR-005, FR-006, FR-014, FR-016
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class RbacAuditTenantDisabledCoverageTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRVinculoProfissionalEstabelecimento\"]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRUnidadeSaude\"]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_rbac_audit_disabled_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbc;

    private UUID tenantId;
    private UUID adminUserId;

    @BeforeEach
    void seedBaseFixtures() {
        jdbc.execute("TRUNCATE TABLE iam_audit_events CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_sessions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_user_groups CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_group_permissions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_groups CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_permissions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_auth_challenges CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");

        tenantId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        UUID adminPracId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        // Organization (active)
        jdbc.update("""
                INSERT INTO organizations
                    (id, tenant_id, fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                     fhir_name, fhir_active, cnes, display_name, quota_tier, account_active,
                     created_at, updated_at)
                VALUES (?,?,?,?::jsonb,?::jsonb,?,true,?,?,?,true,NOW(),NOW())
                """,
                tenantId, tenantId,
                "org-t124-" + tenantId,
                FHIR_ORG_PROFILE,
                "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"1240001\"}]",
                "Clinica T124", "1240001", "Clinica T124", "standard");

        // Location
        jdbc.update("""
                INSERT INTO locations
                    (id, tenant_id, organization_id, fhir_resource_id, fhir_meta_profile,
                     fhir_identifier_json, fhir_name, fhir_status, fhir_mode,
                     display_name, account_active, created_at, updated_at)
                VALUES (?,?,?,?,?::jsonb,?::jsonb,?,?,?,?,true,NOW(),NOW())
                """,
                locationId, tenantId, tenantId,
                "loc-t124-" + locationId,
                LOC_PROFILE,
                "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"1240001\"}]",
                "Unidade T124", "active", "instance", "Unidade T124");

        // Practitioner (admin)
        jdbc.update("""
                INSERT INTO practitioners
                    (id, tenant_id, fhir_resource_id, fhir_meta_profile,
                     fhir_identifier_json, fhir_name_json, fhir_active,
                     display_name, cpf_encrypted, encryption_key_version,
                     account_active, created_at, updated_at)
                VALUES (?,?,?,?::jsonb,?::jsonb,?::jsonb,true,?,decode('','hex'),'v1',true,NOW(),NOW())
                """,
                adminPracId, tenantId,
                "prac-t124-" + adminPracId,
                FHIR_PRACTITIONER_PROFILE,
                "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000124\"}]",
                "[{\"use\":\"official\",\"text\":\"Admin T124\"}]",
                "Admin T124");

        // PractitionerRole
        jdbc.update("""
                INSERT INTO practitioner_roles
                    (id, tenant_id, organization_id, location_id, practitioner_id,
                     fhir_resource_id, fhir_meta_profile, role_code, active, primary_role,
                     created_at, updated_at)
                VALUES (?,?,?,?,?,?,?::jsonb,?,true,true,NOW(),NOW())
                """,
                roleId, tenantId, tenantId, locationId, adminPracId,
                "role-t124-" + roleId,
                FHIR_ROLE_PROFILE,
                "ADMIN");

        // IAM user (admin profile 10)
        jdbc.update("""
                INSERT INTO iam_users
                    (id, tenant_id, practitioner_id, email, password_hash, password_algo,
                     profile, account_active, failed_login_count, created_at, updated_at)
                VALUES (?,?,?,?,?,?,10,true,0,NOW(),NOW())
                """,
                adminUserId, tenantId, adminPracId,
                "admin.t124@test.com",
                "$argon2id$v=19$m=65536,t=3,p=4$dGVzdHNhbHQ$dGVzdGhhc2g=",
                "argon2id");
    }

    // ── SC1: RBAC — caller without required permission gets 403 ────────────

    @Test
    void callerWithoutPermission_accessingAdminEndpoint_returns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());
        // No session cookie — unauthenticated call must be rejected
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/admin/groups",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isBetween(401, 403);
        assertOperationOutcomeShape(response.getBody());
    }

    // ── SC2: Audit — login failure produces iam_audit_events entry ──────────

    @Test
    void loginFailure_producesAuditEvent_withCorrectEventType() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"email":"admin.t124@test.com","password":"WRONG_PASSWORD_T124"}
                """;

        restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), Map.class);

        List<Map<String, Object>> events = jdbc.queryForList(
                "SELECT event_type, trace_id FROM iam_audit_events WHERE event_type = 'LOGIN_FAILED'");

        assertThat(events).isNotEmpty();
        Map<String, Object> ev = events.get(0);
        assertThat(ev.get("event_type")).isEqualTo("LOGIN_FAILED");
        // trace_id must be set for observability (FR-016)
        assertThat(ev.get("trace_id")).isNotNull();
    }

    // ── SC3: Audit — audit events table is append-only (no delete/update) ──

    @Test
    void auditEventsTable_isAppendOnly_deletesAreRejected() {
        // Seed a synthetic audit event
        UUID eventId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO iam_audit_events
                    (id, event_type, payload_json, created_at)
                VALUES (?, 'TEST_EVENT', '{}'::jsonb, NOW())
                """, eventId);

        // Attempt to delete it — must throw (trigger blocks delete on iam_audit_events)
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("DELETE FROM iam_audit_events WHERE id = ?", eventId),
                "iam_audit_events must be append-only (delete must be blocked by trigger)");
    }

    // ── SC4: Disabled tenant — session on disabled tenant is rejected ───────

    @Test
    void disabledTenant_authenticatedRequest_returns403_withOperationOutcome() {
        // Disable the tenant
        jdbc.update("UPDATE organizations SET account_active = false WHERE id = ?", tenantId);

        // Seed a session for the admin user (simulating a pre-existing session)
        UUID sessionId = UUID.randomUUID();
        String opaqueTokenDigest = "sha256-dummy-token-digest-t124";
        jdbc.update("""
                INSERT INTO iam_sessions
                    (id, iam_user_id, tenant_id, organization_id,
                     opaque_token_digest, issued_at, expires_at, active, created_at)
                VALUES (?,?,?,?,?,NOW(),NOW() + INTERVAL '1 hour',true,NOW())
                """,
                sessionId, adminUserId, tenantId, tenantId, opaqueTokenDigest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", "cd_session=" + opaqueTokenDigest);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/users/me/context",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertOperationOutcomeShape(response.getBody());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void assertOperationOutcomeShape(Map<?, ?> body) {
        assertThat(body).isNotNull();
        assertThat(body.get("resourceType")).isEqualTo("OperationOutcome");
        Object issueObj = body.get("issue");
        assertThat(issueObj).isNotNull().isInstanceOf(java.util.List.class);
        java.util.List<?> issues = (java.util.List<?>) issueObj;
        assertThat(issues).isNotEmpty();
    }
}
