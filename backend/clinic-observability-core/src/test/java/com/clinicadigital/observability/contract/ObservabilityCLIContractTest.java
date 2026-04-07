package com.clinicadigital.observability.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T077 [US2] Contract test for observability CLI command surface.
 */
class ObservabilityCLIContractTest {

    private static final Path OBSERVABILITY_COMMANDS = Path.of("src/main/java/com/clinicadigital/observability/cli/ObservabilityCommands.java");

    @Test
    void observabilityCliMustExposeTraceValidateAndMetricsExportWithJson() throws IOException {
        String source = Files.readString(OBSERVABILITY_COMMANDS);

        assertAll(
                "observability-core CLI contract (FR-004, Art. II)",
                () -> assertTrue(source.contains("key = \"trace validate\""), "missing trace validate command"),
                () -> assertTrue(source.contains("key = \"metrics export\""), "missing metrics export command"),
                () -> assertTrue(source.contains("--trace-id"), "trace validate must accept --trace-id"),
                () -> assertTrue(source.contains("--json"), "commands must support --json output")
        );
    }
}