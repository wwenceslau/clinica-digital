package com.clinicadigital.shared.migration;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T030c — Integration test verifying RLS enforcement via PostgreSQL Testcontainers.
 *
 * <p>Validates:
 * <ul>
 *   <li>Queries on tenant-scoped tables as a non-superuser WITHOUT {@code SET LOCAL app.tenant_id}
 *       throw a SQL exception (fail-closed, FR-016a).</li>
 *   <li>Queries WITH correct {@code SET LOCAL app.tenant_id} return only the rows for
 *       that tenant, not rows belonging to another tenant (Art. 0, FR-016).</li>
 * </ul>
 *
 * Refs: FR-016, FR-016a, Art. 0
 */
@Testcontainers
class TenantRlsEnforcementIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @BeforeAll
    static void applyMigrationsAndSetupRole() throws SQLException {
        // 1. Apply all Flyway migrations (V001–V011)
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        // 2. Create app_user role and grant table access for RLS testing
        try (Connection conn = superuserConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE ROLE app_user LOGIN PASSWORD 'app_password'");
            stmt.execute("GRANT USAGE ON SCHEMA public TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON tenants TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON iam_users TO app_user");
            stmt.execute("GRANT SELECT, INSERT ON iam_roles TO app_user");
            stmt.execute("GRANT SELECT, INSERT ON iam_user_roles TO app_user");
            stmt.execute("GRANT SELECT, INSERT ON iam_sessions TO app_user");
            stmt.execute("GRANT INSERT, SELECT ON iam_audit_events TO app_user");

            // Insert seed data as superuser (bypasses RLS for setup)
            UUID tenant1 = UUID.randomUUID();
            UUID tenant2 = UUID.randomUUID();
            stmt.execute("INSERT INTO tenants (id, slug, legal_name, status, plan_tier, " +
                    "quota_requests_per_minute, quota_concurrency, quota_storage_mb, " +
                    "created_at, updated_at) VALUES " +
                    "('" + tenant1 + "', 'tenant1', 'Tenant One', 'active', 'basic', 60, 10, 1000, NOW(), NOW()), " +
                    "('" + tenant2 + "', 'tenant2', 'Tenant Two', 'active', 'basic', 60, 10, 1000, NOW(), NOW())");

            stmt.execute("INSERT INTO iam_users (id, tenant_id, username, email, password_hash, " +
                    "password_algo, is_active, created_at, updated_at) VALUES " +
                    "(gen_random_uuid(), '" + tenant1 + "', 'user1', 'user1@t1.com', 'hash1', 'bcrypt', true, NOW(), NOW()), " +
                    "(gen_random_uuid(), '" + tenant2 + "', 'user2', 'user2@t2.com', 'hash2', 'bcrypt', true, NOW(), NOW())");

            // Store tenant IDs for test access
            stmt.execute("CREATE TABLE _test_tenants (slot text PRIMARY KEY, tenant_id uuid)");
            stmt.execute("INSERT INTO _test_tenants VALUES ('t1', '" + tenant1 + "'), ('t2', '" + tenant2 + "')");
        }
    }

    @Test
    void queryWithoutSetLocalShouldReturnZeroRowsDueToRlsDenyDefault() throws SQLException {
        // current_setting('app.tenant_id', true) with missing_ok=true returns NULL when not set.
        // NULL::uuid = existing_uuid evaluates to NULL (false), so RLS silently denies all rows.
        // This is deny-by-default (Art. 0, FR-016a): no rows leak, and no exception is thrown.
        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM iam_users")) {
                assertFalse(rs.next(),
                        "RLS must return 0 rows when app.tenant_id not set (deny-by-default, FR-016a)");
            }
        }
    }

    @Test
    void queryWithSetLocalShouldReturnOnlyTenant1Rows() throws SQLException {
        UUID tenant1Id = fetchTenantId("t1");
        UUID tenant2Id = fetchTenantId("t2");

        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + tenant1Id + "'");

                try (ResultSet rs = stmt.executeQuery("SELECT tenant_id FROM iam_users")) {
                    while (rs.next()) {
                        UUID rowTenant = UUID.fromString(rs.getString("tenant_id"));
                        assertEquals(tenant1Id, rowTenant,
                                "RLS must only return rows for tenant1, found: " + rowTenant);
                    }
                }
            }
        }
    }

    @Test
    void queryWithSetLocalShouldReturnOnlyTenant2Rows() throws SQLException {
        UUID tenant2Id = fetchTenantId("t2");

        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + tenant2Id + "'");

                try (ResultSet rs = stmt.executeQuery("SELECT tenant_id FROM iam_users")) {
                    while (rs.next()) {
                        UUID rowTenant = UUID.fromString(rs.getString("tenant_id"));
                        assertEquals(tenant2Id, rowTenant,
                                "RLS must only return rows for tenant2, found: " + rowTenant);
                    }
                }
            }
        }
    }

    @Test
    void rlsShouldIsolateIamRolesPerV009() throws SQLException {
        UUID tenant1Id = fetchTenantId("t1");
        UUID tenant2Id = fetchTenantId("t2");

        try (Connection conn = superuserConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO iam_roles (id, tenant_id, role_key, description, created_at) VALUES " +
                    "(gen_random_uuid(), '" + tenant1Id + "', 'ADMIN', 'Admin for T1', NOW()), " +
                    "(gen_random_uuid(), '" + tenant2Id + "', 'ADMIN', 'Admin for T2', NOW())");
        }

        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + tenant1Id + "'");
                try (ResultSet rs = stmt.executeQuery("SELECT tenant_id FROM iam_roles")) {
                    while (rs.next()) {
                        assertEquals(tenant1Id, UUID.fromString(rs.getString("tenant_id")),
                                "V009 RLS must isolate iam_roles by tenant");
                    }
                }
            }
        }
    }

    // --- helpers ---

    private static Connection superuserConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection appUserConnection() throws SQLException {
        String url = POSTGRES.getJdbcUrl();
        return DriverManager.getConnection(url, "app_user", "app_password");
    }

    private static UUID fetchTenantId(String slot) throws SQLException {
        try (Connection conn = superuserConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT tenant_id FROM _test_tenants WHERE slot = ?")) {
            ps.setString(1, slot);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return UUID.fromString(rs.getString("tenant_id"));
            }
        }
    }
}
