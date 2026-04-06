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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T030d — Cross-tenant session isolation integration test.
 *
 * <p>Validates that iam_sessions are strictly isolated between tenants and that
 * credential renewal for one tenant does not affect session visibility for another tenant
 * (AC-SEC-001, FR-016, Art. 0).
 *
 * <p>Scenarios:
 * <ol>
 *   <li>Tenant A's sessions are invisible under Tenant B's RLS context.</li>
 *   <li>Tenant B's sessions are invisible under Tenant A's RLS context.</li>
 *   <li>After credential renewal for Tenant A, Tenant B sessions remain unchanged.</li>
 *   <li>Tenant A's new session token is not accessible under Tenant B's context.</li>
 * </ol>
 *
 * Refs: AC-SEC-001, FR-016, Art. 0
 */
@Testcontainers
class CrossTenantSessionIsolationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_session_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static UUID TENANT_A;
    static UUID TENANT_B;
    static UUID USER_A;
    static UUID USER_B;

    @BeforeAll
    static void applyMigrationsAndSeedData() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection conn = superuserConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE ROLE app_user LOGIN PASSWORD 'app_password'");
            stmt.execute("GRANT USAGE ON SCHEMA public TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON tenants TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON iam_users TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON iam_sessions TO app_user");
            stmt.execute("GRANT SELECT, INSERT ON iam_audit_events TO app_user");
        }

        TENANT_A = UUID.randomUUID();
        TENANT_B = UUID.randomUUID();
        USER_A = UUID.randomUUID();
        USER_B = UUID.randomUUID();

        try (Connection conn = superuserConnection(); Statement stmt = conn.createStatement()) {
            // Insert tenants
            stmt.execute("INSERT INTO tenants (id, slug, legal_name, status, plan_tier, " +
                    "quota_requests_per_minute, quota_concurrency, quota_storage_mb, " +
                    "created_at, updated_at) VALUES " +
                    "('" + TENANT_A + "', 'ta', 'Tenant A', 'active', 'basic', 60, 10, 1000, NOW(), NOW()), " +
                    "('" + TENANT_B + "', 'tb', 'Tenant B', 'active', 'basic', 60, 10, 1000, NOW(), NOW())");

            // Insert users
            stmt.execute("INSERT INTO iam_users (id, tenant_id, username, email, password_hash, " +
                    "password_algo, is_active, created_at, updated_at) VALUES " +
                    "('" + USER_A + "', '" + TENANT_A + "', 'alice', 'alice@ta.com', 'h', 'bcrypt', true, NOW(), NOW()), " +
                    "('" + USER_B + "', '" + TENANT_B + "', 'bob', 'bob@tb.com', 'h', 'bcrypt', true, NOW(), NOW())");
        }
    }

    @Test
    void tenantAShouldNotSeeSessionsFromTenantB() throws SQLException {
        UUID sessionA = insertSession(USER_A, TENANT_A);
        UUID sessionB = insertSession(USER_B, TENANT_B);

        List<UUID> visibleSessions = queryVisibleSessionIds(TENANT_A);

        assertTrue(visibleSessions.contains(sessionA),
                "Tenant A must see its own session");
        assertFalse(visibleSessions.contains(sessionB),
                "Tenant A must NOT see Tenant B's session (RLS isolation)");
    }

    @Test
    void tenantBShouldNotSeeSessionsFromTenantA() throws SQLException {
        UUID sessionA = insertSession(USER_A, TENANT_A);
        UUID sessionB = insertSession(USER_B, TENANT_B);

        List<UUID> visibleSessions = queryVisibleSessionIds(TENANT_B);

        assertTrue(visibleSessions.contains(sessionB),
                "Tenant B must see its own session");
        assertFalse(visibleSessions.contains(sessionA),
                "Tenant B must NOT see Tenant A's session (RLS isolation)");
    }

    @Test
    void credentialRenewalForTenantAShouldNotAffectTenantBSessions() throws SQLException {
        UUID oldSessionA = insertSession(USER_A, TENANT_A);
        UUID sessionB = insertSession(USER_B, TENANT_B);

        // Simulate credential renewal for Tenant A: expire old session, issue new one
        expireSession(oldSessionA, TENANT_A);
        UUID newSessionA = insertSession(USER_A, TENANT_A);

        List<UUID> tenantBVisible = queryVisibleSessionIds(TENANT_B);

        assertTrue(tenantBVisible.contains(sessionB),
                "Tenant B session must still be visible to Tenant B after Tenant A renewal");
        assertFalse(tenantBVisible.contains(oldSessionA),
                "Tenant A's old session must not be visible to Tenant B");
        assertFalse(tenantBVisible.contains(newSessionA),
                "Tenant A's new session must not be visible to Tenant B");
    }

    @Test
    void tenantATokenMustNotBeAccessibleUnderTenantBContext() throws SQLException {
        UUID sessionA = insertSession(USER_A, TENANT_A);

        // Try to look up session A's token under Tenant B's RLS context
        boolean foundUnderTenantB = lookupSessionById(sessionA, TENANT_B);

        assertFalse(foundUnderTenantB,
                "Tenant A's session token must not be accessible when querying under Tenant B context (AC-SEC-001)");
    }

    // --- helpers ---

    private static UUID insertSession(UUID userId, UUID tenantId) throws SQLException {
        UUID sessionId = UUID.randomUUID();
        String expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).toString();
        try (Connection conn = superuserConnection();
             Statement stmt = conn.createStatement()) {
            // iam_sessions schema: id, tenant_id, user_id, issued_at, expires_at, revoked_at, ip, user_agent, trace_id
            stmt.execute("INSERT INTO iam_sessions (id, tenant_id, user_id, expires_at) VALUES " +
                    "('" + sessionId + "', '" + tenantId + "', '" + userId + "', " +
                    "'" + expiresAt + "')");
        }
        return sessionId;
    }

    private static void expireSession(UUID sessionId, UUID tenantId) throws SQLException {
        try (Connection conn = superuserConnection();
             Statement stmt = conn.createStatement()) {
            // Revoke by setting revoked_at (iam_sessions has no is_revoked boolean column)
            stmt.execute("UPDATE iam_sessions SET revoked_at = NOW() WHERE id = '" + sessionId + "'");
        }
    }

    private static List<UUID> queryVisibleSessionIds(UUID tenantId) throws SQLException {
        List<UUID> result = new ArrayList<>();
        try (Connection conn = appUserConnection()) {
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

    private static boolean lookupSessionById(UUID sessionId, UUID contextTenantId) throws SQLException {
        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + contextTenantId + "'");
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM iam_sessions WHERE id = ?")) {
                    ps.setObject(1, sessionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        }
    }

    private static Connection superuserConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection appUserConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), "app_user", "app_password");
    }
}
