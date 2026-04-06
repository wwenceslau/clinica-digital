package com.clinicadigital.gateway.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * T041 — Integration test: verifies that RLS isolation is automatically enforced
 * across ALL tenant-scoped tables when the gateway application's migration set is applied.
 *
 * <p>Scope: This test validates the DB-layer contract that underpins the entire gateway's
 * tenant isolation guarantee (Art. 0, FR-001, FR-002). It applies the complete migration
 * set (V001–V011) and verifies that:
 * <ul>
 *   <li>Two tenants' data are invisible to each other at the DB level.</li>
 *   <li>All tenant-scoped tables (iam_users, iam_roles, iam_sessions) enforce RLS.</li>
 *   <li>Inserting data for Tenant A and querying as Tenant B returns 0 rows (deny-by-default).</li>
 * </ul>
 *
 * <p><b>TDD state</b>: GREEN from the start — this validates the DB baseline that all
 * Phase 3.B/C/D implementations must rely on. Acts as a regression contract protecting
 * against accidental RLS policy removal.
 *
 * Refs: Art. 0, FR-001, FR-002, FR-016
 */
@Testcontainers
class TenantIsolationRLSTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_isolation_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static UUID TENANT_A;
    static UUID TENANT_B;
    static UUID USER_A;
    static UUID USER_B;

    @BeforeAll
    static void applyMigrationsAndBootstrap() throws SQLException {
        // Apply all migrations from clinic-shared-kernel classpath (V001–V011)
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        // Create app_user role for RLS-constrained queries (non-superuser)
        try (Connection conn = adminConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE ROLE app_user LOGIN PASSWORD 'app_password'");
            stmt.execute("GRANT USAGE ON SCHEMA public TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON tenants TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON iam_users TO app_user");
            stmt.execute("GRANT SELECT, INSERT ON iam_roles TO app_user");
            stmt.execute("GRANT SELECT, INSERT ON iam_sessions TO app_user");
        }

        // Seed: two tenants + one user each (as superuser, bypassing RLS for setup)
        TENANT_A = UUID.randomUUID();
        TENANT_B = UUID.randomUUID();
        USER_A = UUID.randomUUID();
        USER_B = UUID.randomUUID();

        try (Connection conn = adminConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO tenants (id, slug, legal_name, status, plan_tier, " +
                    "quota_requests_per_minute, quota_concurrency, quota_storage_mb, " +
                    "created_at, updated_at) VALUES " +
                    "('" + TENANT_A + "', 'acme', 'Acme Corp', 'active', 'basic', 60, 10, 1024, NOW(), NOW()), " +
                    "('" + TENANT_B + "', 'beto', 'Beto Ltda', 'active', 'basic', 60, 10, 1024, NOW(), NOW())");

            stmt.execute("INSERT INTO iam_users (id, tenant_id, username, email, password_hash, " +
                    "password_algo, is_active, created_at, updated_at) VALUES " +
                    "('" + USER_A + "', '" + TENANT_A + "', 'alice', 'alice@acme.com', 'hash_a', 'bcrypt', true, NOW(), NOW()), " +
                    "('" + USER_B + "', '" + TENANT_B + "', 'bob', 'bob@beto.com', 'hash_b', 'bcrypt', true, NOW(), NOW())");

            stmt.execute("INSERT INTO iam_roles (id, tenant_id, role_key, description, created_at) VALUES " +
                    "(gen_random_uuid(), '" + TENANT_A + "', 'ADMIN', 'Admin role for A', NOW()), " +
                    "(gen_random_uuid(), '" + TENANT_B + "', 'ADMIN', 'Admin role for B', NOW())");

            stmt.execute("INSERT INTO iam_sessions (id, tenant_id, user_id, expires_at) VALUES " +
                    "(gen_random_uuid(), '" + TENANT_A + "', '" + USER_A + "', NOW() + INTERVAL '1 hour'), " +
                    "(gen_random_uuid(), '" + TENANT_B + "', '" + USER_B + "', NOW() + INTERVAL '1 hour')");
        }
    }

    // --- iam_users isolation ---

    @Test
    void tenantAQueriesOnlyIamUsersOfTenantA() throws SQLException {
        List<UUID> visible = queryIamUserTenantIds(TENANT_A);
        assertThat(visible).allMatch(TENANT_A::equals);
        assertThat(visible).hasSize(1);
    }

    @Test
    void tenantBQueriesOnlyIamUsersOfTenantB() throws SQLException {
        List<UUID> visible = queryIamUserTenantIds(TENANT_B);
        assertThat(visible).allMatch(TENANT_B::equals);
        assertThat(visible).hasSize(1);
    }

    @Test
    void tenantACannotSeeIamUsersOfTenantB() throws SQLException {
        List<UUID> visible = queryIamUserTenantIds(TENANT_A);
        assertThat(visible).doesNotContain(TENANT_B);
    }

    // --- iam_roles isolation ---

    @Test
    void rlsIsolatesIamRolesPerTenant() throws SQLException {
        List<UUID> visibleForA = queryIamRoleTenantIds(TENANT_A);
        List<UUID> visibleForB = queryIamRoleTenantIds(TENANT_B);

        assertThat(visibleForA).allMatch(TENANT_A::equals);
        assertThat(visibleForB).allMatch(TENANT_B::equals);
    }

    // --- iam_sessions isolation ---

    @Test
    void rlsIsolatesIamSessionsPerTenant() throws SQLException {
        List<UUID> visibleForA = queryIamSessionTenantIds(TENANT_A);
        List<UUID> visibleForB = queryIamSessionTenantIds(TENANT_B);

        assertThat(visibleForA).allMatch(TENANT_A::equals);
        assertThat(visibleForB).allMatch(TENANT_B::equals);
    }

    // --- deny-by-default ---

    @Test
    void queryWithoutSetLocalReturnsZeroRowsDenyByDefault() throws SQLException {
        // current_setting('app.tenant_id', true) returns NULL → NULL::uuid comparison → deny-by-default
        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM iam_users")) {
                assertFalse(rs.next(), "RLS deny-by-default: 0 rows when tenant_id not set");
            }
        }
    }

    // --- helpers ---

    private static List<UUID> queryIamUserTenantIds(UUID tenantId) throws SQLException {
        return queryTenantIds("SELECT tenant_id FROM iam_users", tenantId);
    }

    private static List<UUID> queryIamRoleTenantIds(UUID tenantId) throws SQLException {
        return queryTenantIds("SELECT tenant_id FROM iam_roles", tenantId);
    }

    private static List<UUID> queryIamSessionTenantIds(UUID tenantId) throws SQLException {
        return queryTenantIds("SELECT tenant_id FROM iam_sessions", tenantId);
    }

    private static List<UUID> queryTenantIds(String sql, UUID tenantId) throws SQLException {
        List<UUID> result = new ArrayList<>();
        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        result.add(UUID.fromString(rs.getString("tenant_id")));
                    }
                }
            }
        }
        return result;
    }

    private static Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection appUserConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), "app_user", "app_password");
    }
}
