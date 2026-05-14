package com.clinicadigital.gateway.integration;

import com.clinicadigital.gateway.security.LoginLockoutService;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T157 [Phase 18] - Lockout state must be persisted in iam_users.failed_login_count/locked_until.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class LoginLockoutPersistenceIntegrationTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_lockout_persistence_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    LoginLockoutService loginLockoutService;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    @Test
    void registerFailureMustPersistFailedCountAndLockAfterThreshold() {
        UUID tenantId = insertTenantAndOrganization();
        String email = "lockout@test.local";
        insertUser(tenantId, email);

        String key = tenantId + ":" + email;

        for (int i = 0; i < 4; i++) {
            loginLockoutService.registerFailure(key);
        }

        Integer failedAfter4 = jdbc.queryForObject(
                "SELECT failed_login_count FROM iam_users WHERE tenant_id = ? AND email = ?",
                Integer.class,
                tenantId, email);
        Timestamp lockedAfter4 = jdbc.queryForObject(
                "SELECT locked_until FROM iam_users WHERE tenant_id = ? AND email = ?",
                Timestamp.class,
                tenantId, email);

        assertThat(failedAfter4).isEqualTo(4);
        assertThat(lockedAfter4).isNull();

        loginLockoutService.registerFailure(key);

        Integer failedAfter5 = jdbc.queryForObject(
                "SELECT failed_login_count FROM iam_users WHERE tenant_id = ? AND email = ?",
                Integer.class,
                tenantId, email);
        Timestamp lockedAfter5 = jdbc.queryForObject(
                "SELECT locked_until FROM iam_users WHERE tenant_id = ? AND email = ?",
                Timestamp.class,
                tenantId, email);

        assertThat(failedAfter5).isEqualTo(5);
        assertThat(lockedAfter5).isNotNull();
        assertThat(lockedAfter5.toInstant()).isAfter(Instant.now());

        assertThatThrownBy(() -> loginLockoutService.assertNotLocked(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too many login attempts");
    }

    @Test
    void registerSuccessMustClearPersistedLockoutState() {
        UUID tenantId = insertTenantAndOrganization();
        String email = "unlock@test.local";
        UUID userId = insertUser(tenantId, email);

        jdbc.update("""
                UPDATE iam_users
                SET failed_login_count = 5,
                    locked_until = NOW() + INTERVAL '10 minutes'
                WHERE id = ?
                """, userId);

        loginLockoutService.registerSuccess(tenantId + ":" + email);

        Integer failed = jdbc.queryForObject(
                "SELECT failed_login_count FROM iam_users WHERE id = ?",
                Integer.class,
                userId);
        Timestamp lockedUntil = jdbc.queryForObject(
                "SELECT locked_until FROM iam_users WHERE id = ?",
                Timestamp.class,
                userId);

        assertThat(failed).isZero();
        assertThat(lockedUntil).isNull();
    }

    private UUID insertTenantAndOrganization() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tenants (id, slug, legal_name, status, plan_tier,
                    quota_requests_per_minute, quota_concurrency, quota_storage_mb,
                    created_at, updated_at)
                VALUES (?, ?, ?, 'active', 'standard', 120, 10, 1024, NOW(), NOW())
                """, id, "lock-" + id.toString().substring(0, 8), "Lockout Tenant");

        jdbc.update("""
                INSERT INTO organizations (id, tenant_id, cnes, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_active, account_active, created_at, updated_at)
                VALUES (?, ?, '7654321', 'Lockout Org', ?, ?::jsonb,
                    '[{"system":"https://saude.gov.br/sid/cnes","value":"7654321"}]'::jsonb,
                    'Lockout Org', true, true, NOW(), NOW())
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
}
