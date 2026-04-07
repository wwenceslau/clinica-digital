package com.clinicadigital.tenant.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T067 [US2] Contract test for tenant-core CLI surface.
 *
 * This contract locks the expected command set for modular CLI operation.
 */
class TenantCLIContractTest {

    private static final Path TENANT_COMMANDS = Path.of("src/main/java/com/clinicadigital/tenant/cli/TenantCommands.java");
    private static final Path QUOTA_COMMANDS = Path.of("src/main/java/com/clinicadigital/tenant/cli/QuotaCommands.java");

    @Test
    void tenantCommandsMustExposeExpectedSurfaceAndJsonOutput() throws IOException {
        String tenantSource = Files.readString(TENANT_COMMANDS);
        String quotaSource = Files.readString(QUOTA_COMMANDS);

        assertAll(
                "tenant-core CLI contract (Art. II, FR-004)",
                () -> assertTrue(tenantSource.contains("key = \"tenant create\""), "missing tenant create command"),
                () -> assertTrue(tenantSource.contains("key = \"tenant list\""), "missing tenant list command"),
                () -> assertTrue(tenantSource.contains("key = \"tenant quota update\""), "missing tenant quota update command"),
                () -> assertTrue(tenantSource.contains("key = \"tenant block\""), "missing tenant block command"),
                () -> assertTrue(tenantSource.contains("key = \"tenant unblock\""), "missing tenant unblock command"),
                () -> assertTrue(quotaSource.contains("key = \"quota check\""), "missing quota check command"),
                () -> assertTrue(tenantSource.contains("--json"), "tenant commands must support --json output"),
                () -> assertTrue(quotaSource.contains("--json"), "quota commands must support --json output")
        );
    }
}
