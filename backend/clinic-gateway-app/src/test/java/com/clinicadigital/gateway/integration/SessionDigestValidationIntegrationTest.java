package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.application.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
 * T156 [Phase 18] - Session validation must resolve by opaque_token_digest, not by iam_sessions.id.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class SessionDigestValidationIntegrationTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_session_digest_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SessionManager sessionManager;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE TABLE iam_sessions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    @Test
    void validateSessionMustUseDigestEvenWhenTokenDiffersFromRowId() {
        UUID tenantId = insertTenantAndOrganization();
        UUID userId = insertUser(tenantId, "digest@test.local");

        UUID dbRowId = UUID.randomUUID();
        UUID opaqueToken = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO iam_sessions (
                    id, tenant_id, organization_id, iam_user_id,
                    opaque_token_digest, active, issued_at, created_at, expires_at
                ) VALUES (?, ?, ?, ?, ?, true, NOW(), NOW(), NOW() + INTERVAL '30 minutes')
                """,
                dbRowId, tenantId, tenantId, userId, sha256Hex(opaqueToken.toString()));

        boolean validWithOpaqueToken = sessionManager.validateSession(opaqueToken, tenantId);
        boolean validWithDbId = sessionManager.validateSession(dbRowId, tenantId);

        assertThat(validWithOpaqueToken).isTrue();
        assertThat(validWithDbId).isFalse();
    }

    private UUID insertTenantAndOrganization() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tenants (id, slug, legal_name, status, plan_tier,
                    quota_requests_per_minute, quota_concurrency, quota_storage_mb,
                    created_at, updated_at)
                VALUES (?, ?, ?, 'active', 'standard', 120, 10, 1024, NOW(), NOW())
                """, id, "digest-" + id.toString().substring(0, 8), "Digest Tenant");

        jdbc.update("""
                INSERT INTO organizations (id, tenant_id, cnes, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_active, account_active, created_at, updated_at)
                VALUES (?, ?, '1234567', 'Digest Org', ?, ?::jsonb,
                    '[{"system":"https://saude.gov.br/sid/cnes","value":"1234567"}]'::jsonb,
                    'Digest Org', true, true, NOW(), NOW())
                """, id, id, "org-" + id, FHIR_ORG_PROFILE);
        return id;
    }

    private UUID insertUser(UUID tenantId, String email) {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO iam_users (id, tenant_id, username, email, password_hash,
                    password_algo, account_active, profile, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'hash', 'bcrypt', true, 10, NOW(), NOW())
                """, userId, tenantId, email, email);
        return userId;
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
