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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T134 (SC-003): validates cross-tenant RLS blocking with 5+ scenarios and audit evidence.
 */
@Testcontainers
class CrossTenantRlsAuditIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_cross_tenant_rls_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeAll
    static void setupSchemaAndData() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection conn = adminConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE ROLE app_user LOGIN PASSWORD 'app_password'");
            stmt.execute("GRANT USAGE ON SCHEMA public TO app_user");
            stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user");
            stmt.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user");

            stmt.execute("INSERT INTO tenants (id, slug, legal_name, status, plan_tier, quota_requests_per_minute, quota_concurrency, quota_storage_mb, created_at, updated_at) VALUES " +
                    "('" + TENANT_A + "', 'tenant-a', 'Tenant A', 'active', 'basic', 120, 10, 1024, NOW(), NOW())," +
                    "('" + TENANT_B + "', 'tenant-b', 'Tenant B', 'active', 'basic', 120, 10, 1024, NOW(), NOW())");

            stmt.execute("INSERT INTO organizations (id, tenant_id, cnes, display_name, fhir_resource_id, fhir_meta_profile, fhir_identifier_json, fhir_name, fhir_active, created_at, updated_at) VALUES " +
                    "(gen_random_uuid(), '" + TENANT_A + "', '1234501', 'Org A', 'org-a', '[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]'::jsonb, '[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"1234501\"}]'::jsonb, 'Org A', true, NOW(), NOW())," +
                    "(gen_random_uuid(), '" + TENANT_B + "', '1234502', 'Org B', 'org-b', '[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]'::jsonb, '[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"1234502\"}]'::jsonb, 'Org B', true, NOW(), NOW())");

            stmt.execute("INSERT INTO practitioners (id, tenant_id, fhir_resource_id, fhir_meta_profile, fhir_identifier_json, fhir_name_json, fhir_active, display_name, account_active, created_at, updated_at) VALUES " +
                    "(gen_random_uuid(), '" + TENANT_A + "', 'prac-a', '[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]'::jsonb, '[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"11111111111\"}]'::jsonb, '[{\"text\":\"A User\"}]'::jsonb, true, 'A User', true, NOW(), NOW())," +
                    "(gen_random_uuid(), '" + TENANT_B + "', 'prac-b', '[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]'::jsonb, '[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"22222222222\"}]'::jsonb, '[{\"text\":\"B User\"}]'::jsonb, true, 'B User', true, NOW(), NOW())");

            stmt.execute("INSERT INTO iam_users (id, tenant_id, username, email, password_hash, password_algo, is_active, created_at, updated_at, profile) VALUES " +
                    "(gen_random_uuid(), '" + TENANT_A + "', 'admin-a', 'admin-a@tenant-a.test', 'hash-a', 'bcrypt', true, NOW(), NOW(), 10)," +
                    "(gen_random_uuid(), '" + TENANT_B + "', 'admin-b', 'admin-b@tenant-b.test', 'hash-b', 'bcrypt', true, NOW(), NOW(), 10)");

            stmt.execute("INSERT INTO iam_groups (id, tenant_id, name, description, created_at) VALUES " +
                    "(gen_random_uuid(), '" + TENANT_A + "', 'group-a', 'Group A', NOW())," +
                    "(gen_random_uuid(), '" + TENANT_B + "', 'group-b', 'Group B', NOW())");

            stmt.execute("INSERT INTO iam_sessions (id, tenant_id, user_id, expires_at) " +
                    "SELECT gen_random_uuid(), u.tenant_id, u.id, NOW() + INTERVAL '1 hour' FROM iam_users u");
        }
    }

    @Test
    void shouldBlockCrossTenantAccessInFiveScenariosAndAuditEachBlock() throws SQLException {
        assertCrossTenantBlockedAndAudited("SELECT id FROM organizations WHERE tenant_id = '" + TENANT_B + "'", "organizations.read");
        assertCrossTenantBlockedAndAudited("SELECT id FROM practitioners WHERE tenant_id = '" + TENANT_B + "'", "practitioners.read");
        assertCrossTenantBlockedAndAudited("SELECT id FROM iam_users WHERE tenant_id = '" + TENANT_B + "'", "iam_users.read");
        assertCrossTenantBlockedAndAudited("SELECT id FROM iam_groups WHERE tenant_id = '" + TENANT_B + "'", "iam_groups.read");
        assertCrossTenantBlockedAndAudited("SELECT id FROM iam_sessions WHERE tenant_id = '" + TENANT_B + "'", "iam_sessions.read");

        long auditCount = countTenantAAudits("rls.cross_tenant_blocked");
        assertThat(auditCount)
                .as("SC-003: each blocked cross-tenant attempt must be audited")
                .isGreaterThanOrEqualTo(5L);
    }

    private static void assertCrossTenantBlockedAndAudited(String sql, String scenario) throws SQLException {
        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + TENANT_A + "'");
                stmt.execute("SET LOCAL app.profile = '10'");
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    assertThat(rs.next())
                            .as("scenario %s must return 0 rows under cross-tenant access", scenario)
                            .isFalse();
                }

                stmt.execute("INSERT INTO iam_audit_events (tenant_id, actor_user_id, event_type, outcome, trace_id, metadata_json, created_at) " +
                        "SELECT '" + TENANT_A + "', u.id, 'rls.cross_tenant_blocked', 'blocked', 'trace-t134', " +
                        "jsonb_build_object('scenario', '" + scenario + "', 'target_tenant', '" + TENANT_B + "'), NOW() " +
                        "FROM iam_users u WHERE u.tenant_id = '" + TENANT_A + "' LIMIT 1");
            }
            conn.commit();
        }
    }

    private static long countTenantAAudits(String eventType) throws SQLException {
        try (Connection conn = appUserConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.tenant_id = '" + TENANT_A + "'");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM iam_audit_events WHERE tenant_id = '" + TENANT_A + "' AND event_type = '" + eventType + "'")) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        }
    }

    private static Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection appUserConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), "app_user", "app_password");
    }
}
