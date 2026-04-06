package com.clinicadigital.shared.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationRlsContractTest {

    @Test
    void shouldEnforceRlsForTenantScopedTables() throws IOException {
        String usersRls = Files.readString(Path.of(
                "src/main/resources/db/migration/V005__enable_rls_iam_users.sql"));
        String sessionsRls = Files.readString(Path.of(
                "src/main/resources/db/migration/V006__enable_rls_iam_sessions.sql"));
        String tenantsRls = Files.readString(Path.of(
                "src/main/resources/db/migration/V007__enable_rls_tenants.sql"));
        String rolesRls = Files.readString(Path.of(
                "src/main/resources/db/migration/V009__enable_rls_iam_roles.sql"));
        String userRolesRls = Files.readString(Path.of(
                "src/main/resources/db/migration/V010__enable_rls_iam_user_roles.sql"));
        String auditEventsRls = Files.readString(Path.of(
                "src/main/resources/db/migration/V011__enable_rls_iam_audit_events.sql"));

        assertAll(
                () -> assertTrue(usersRls.contains("ENABLE ROW LEVEL SECURITY")),
                () -> assertTrue(usersRls.contains("FORCE ROW LEVEL SECURITY")),
                () -> assertTrue(usersRls.contains("FOR ALL")),
                () -> assertTrue(usersRls.contains("WITH CHECK")),
                () -> assertTrue(sessionsRls.contains("ENABLE ROW LEVEL SECURITY")),
                () -> assertTrue(sessionsRls.contains("FORCE ROW LEVEL SECURITY")),
                () -> assertTrue(tenantsRls.contains("ENABLE ROW LEVEL SECURITY")),
                () -> assertTrue(tenantsRls.contains("FORCE ROW LEVEL SECURITY")),
                // V009 — iam_roles
                () -> assertTrue(rolesRls.contains("ENABLE ROW LEVEL SECURITY"),
                        "V009 must enable RLS on iam_roles"),
                () -> assertTrue(rolesRls.contains("FORCE ROW LEVEL SECURITY"),
                        "V009 must force RLS on iam_roles"),
                () -> assertTrue(rolesRls.contains("FOR ALL"),
                        "V009 policy must cover all commands"),
                // V010 — iam_user_roles (tenant isolation via JOIN)
                () -> assertTrue(userRolesRls.contains("ENABLE ROW LEVEL SECURITY"),
                        "V010 must enable RLS on iam_user_roles"),
                () -> assertTrue(userRolesRls.contains("FORCE ROW LEVEL SECURITY"),
                        "V010 must force RLS on iam_user_roles"),
                () -> assertTrue(userRolesRls.contains("FOR ALL"),
                        "V010 policy must cover all commands"),
                // V011 — iam_audit_events (append-only + REVOKE UPDATE/DELETE)
                () -> assertTrue(auditEventsRls.contains("ENABLE ROW LEVEL SECURITY"),
                        "V011 must enable RLS on iam_audit_events"),
                () -> assertTrue(auditEventsRls.contains("FORCE ROW LEVEL SECURITY"),
                        "V011 must force RLS on iam_audit_events"),
                () -> assertTrue(auditEventsRls.contains("FOR ALL"),
                        "V011 policy must cover all commands")
        );
    }

    @Test
    void shouldProvideRollbackForEveryMigrationStep() throws IOException {
        Path migrationDir = Path.of("src/main/resources/db/migration");
        Path rollbackDir = Path.of("src/main/resources/db/rollback");

        long migrationCount = Files.list(migrationDir)
                .filter(path -> path.getFileName().toString().startsWith("V"))
                .count();
        long rollbackCount = Files.list(rollbackDir)
                .filter(path -> path.getFileName().toString().startsWith("R"))
                .count();

        assertTrue(migrationCount == rollbackCount,
                "Every migration should have a rollback pair");
    }
}
