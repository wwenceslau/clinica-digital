package com.clinicadigital.gateway.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * T087 [US3] — Integration test: verifies that {@code iam_sessions} table
 * enforces RLS tenant isolation for two distinct tenants.
 *
 * <p><b>TDD state</b>: GREEN — this validates the database-level session isolation
 * contract that the IAM implementation (T092–T108, Phase 5.B) must rely on.
 * It acts as a regression guard ensuring RLS is never accidentally removed from
 * {@code iam_sessions}.
 *
 * <p>Scenarios covered:
 * <ul>
 *   <li>Tenant A's session is returned when queried as Tenant A.</li>
 *   <li>Tenant B's session is returned when queried as Tenant B.</li>
 *   <li>Tenant A's session is invisible when queried as Tenant B (cross-tenant
 *       isolation).</li>
 *   <li>Tenant B's session is invisible when queried as Tenant A.</li>
 *   <li>Querying without {@code SET LOCAL} returns 0 rows (deny-by-default).</li>
 *   <li>A revoked session ({@code revoked_at IS NOT NULL}) is still present in
 *       the persistence layer (revocation is logical, not physical), but is
 *       hidden from the other tenant's RLS context.</li>
 *   <li>An expired session ({@code expires_at} in the past) is present in the
 *       persistence layer but hidden from the other tenant's context.</li>
 * </ul>
 *
 * Refs: Art. 0, FR-007, FR-007a
 */
@Testcontainers
class SessionIsolationRLSTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_session_isolation_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static UUID TENANT_A;
    static UUID TENANT_B;
    static UUID USER_A;
    static UUID USER_B;
    static UUID SESSION_A;
    static UUID SESSION_B;
    static UUID REVOKED_SESSION_A;
    static UUID EXPIRED_SESSION_B;

    @BeforeAll
    static void applyMigrationsAndSeed() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        // Create non-privileged app_user (affected by RLS)
        try (Connection conn = adminConn(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE ROLE app_user LOGIN PASSWORD 'app_password'");
            stmt.execute("GRANT USAGE ON SCHEMA public TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON tenants TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON iam_users TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE ON iam_sessions TO app_user");
        }

        TENANT_A = UUID.randomUUID();
        TENANT_B = UUID.randomUUID();
        USER_A   = UUID.randomUUID();
        USER_B   = UUID.randomUUID();
        SESSION_A         = UUID.randomUUID();
        SESSION_B         = UUID.randomUUID();
        REVOKED_SESSION_A = UUID.randomUUID();
        EXPIRED_SESSION_B = UUID.randomUUID();

        try (Connection conn = adminConn(); Statement stmt = conn.createStatement()) {
            // tenants
            stmt.execute("INSERT INTO tenants (id, slug, legal_name, status, plan_tier, " +
                    "quota_requests_per_minute, quota_concurrency, quota_storage_mb, " +
                    "created_at, updated_at) VALUES " +
                    "('" + TENANT_A + "', 'alpha', 'Alpha Corp', 'active', 'basic', 60, 10, 1024, NOW(), NOW()), " +
                    "('" + TENANT_B + "', 'beta', 'Beta Ltda', 'active', 'basic', 60, 10, 1024, NOW(), NOW())");

            // iam_users
            stmt.execute("INSERT INTO iam_users (id, tenant_id, username, email, password_hash, " +
                    "password_algo, is_active, created_at, updated_at) VALUES " +
                    "('" + USER_A + "', '" + TENANT_A + "', 'alice', 'alice@alpha.com', 'hash_a', 'bcrypt', true, NOW(), NOW()), " +
                    "('" + USER_B + "', '" + TENANT_B + "', 'bob',   'bob@beta.com',   'hash_b', 'bcrypt', true, NOW(), NOW())");

            // active sessions
            stmt.execute("INSERT INTO iam_sessions (id, tenant_id, user_id, expires_at) VALUES " +
                    "('" + SESSION_A + "', '" + TENANT_A + "', '" + USER_A + "', NOW() + INTERVAL '1 hour'), " +
                    "('" + SESSION_B + "', '" + TENANT_B + "', '" + USER_B + "', NOW() + INTERVAL '1 hour')");

            // revoked session for Tenant A
            stmt.execute("INSERT INTO iam_sessions (id, tenant_id, user_id, expires_at, revoked_at) VALUES " +
                    "('" + REVOKED_SESSION_A + "', '" + TENANT_A + "', '" + USER_A + "', " +
                    "NOW() + INTERVAL '1 hour', NOW() - INTERVAL '5 minutes')");

            // expired session for Tenant B
            stmt.execute("INSERT INTO iam_sessions (id, tenant_id, user_id, expires_at) VALUES " +
                    "('" + EXPIRED_SESSION_B + "', '" + TENANT_B + "', '" + USER_B + "', " +
                    "NOW() - INTERVAL '1 hour')");
        }
    }

    // -----------------------------------------------------------------------
    // RLS isolation: each tenant sees own sessions
    // -----------------------------------------------------------------------

    @Test
    void tenantASeesOwnSessions() throws SQLException {
        List<UUID> ids = querySessionIds(TENANT_A);
        assertThat(ids).contains(SESSION_A, REVOKED_SESSION_A);
        assertThat(ids).allMatch(id -> id.equals(SESSION_A) || id.equals(REVOKED_SESSION_A));
    }

    @Test
    void tenantBSeesOwnSessions() throws SQLException {
        List<UUID> ids = querySessionIds(TENANT_B);
        assertThat(ids).contains(SESSION_B, EXPIRED_SESSION_B);
        assertThat(ids).allMatch(id -> id.equals(SESSION_B) || id.equals(EXPIRED_SESSION_B));
    }

    // -----------------------------------------------------------------------
    // Cross-tenant: sessions are completely invisible across contexts
    // -----------------------------------------------------------------------

    @Test
    void tenantASessionsInvisibleToTenantB() throws SQLException {
        List<UUID> visibleToB = querySessionIds(TENANT_B);
        assertThat(visibleToB)
                .as("Tenant A sessions MUST be invisible to Tenant B (Art. 0, FR-007)")
                .doesNotContain(SESSION_A, REVOKED_SESSION_A);
    }

    @Test
    void tenantBSessionsInvisibleToTenantA() throws SQLException {
        List<UUID> visibleToA = querySessionIds(TENANT_A);
        assertThat(visibleToA)
                .as("Tenant B sessions MUST be invisible to Tenant A (Art. 0, FR-007)")
                .doesNotContain(SESSION_B, EXPIRED_SESSION_B);
    }

    // -----------------------------------------------------------------------
    // All tenant_ids visible per context match the querying tenant
    // -----------------------------------------------------------------------

    @Test
    void allTenantIdsUnderTenantAContextBelongToTenantA() throws SQLException {
        List<UUID> tenantIds = querySessionTenantIds(TENANT_A);
        assertThat(tenantIds).isNotEmpty();
        assertThat(tenantIds).allMatch(TENANT_A::equals);
    }

    @Test
    void allTenantIdsUnderTenantBContextBelongToTenantB() throws SQLException {
        List<UUID> tenantIds = querySessionTenantIds(TENANT_B);
        assertThat(tenantIds).isNotEmpty();
        assertThat(tenantIds).allMatch(TENANT_B::equals);
    }

    // -----------------------------------------------------------------------
    // Deny-by-default: no SET LOCAL → 0 rows returned
    // -----------------------------------------------------------------------

    @Test
    void queryWithoutSetLocalReturnsZeroSessionsDenyByDefault() throws SQLException {
        try (Connection conn = appUserConn()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM iam_sessions")) {
                assertFalse(rs.next(),
                        "RLS deny-by-default: iam_sessions must return 0 rows without SET LOCAL (Art. 0)");
            }
        }
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private static List<UUID> querySessionIds(UUID tenantId) throws SQLException {
        List<UUID> result = new ArrayList<>();
        try (Connection conn = appUserConn()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");
                try (ResultSet rs = stmt.executeQuery("SELECT id FROM iam_sessions")) {
                    while (rs.next()) {
                        result.add(UUID.fromString(rs.getString("id")));
                    }
                }
            }
        }
        return result;
    }

    private static List<UUID> querySessionTenantIds(UUID tenantId) throws SQLException {
        List<UUID> result = new ArrayList<>();
        try (Connection conn = appUserConn()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");
                try (ResultSet rs = stmt.executeQuery("SELECT tenant_id FROM iam_sessions")) {
                    while (rs.next()) {
                        result.add(UUID.fromString(rs.getString("tenant_id")));
                    }
                }
            }
        }
        return result;
    }

    private static Connection adminConn() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection appUserConn() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), "app_user", "app_password");
    }
}
