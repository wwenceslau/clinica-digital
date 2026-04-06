package com.clinicadigital.observability;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogbackJsonContractTest {

    @Test
    void shouldIncludeMandatoryStructuredFieldsForObservabilityContract() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/logback-spring.xml"));

        assertAll(
                () -> assertTrue(config.contains("tenant_id")),
                () -> assertTrue(config.contains("trace_id")),
                () -> assertTrue(config.contains("operation")),
                () -> assertTrue(config.contains("outcome")),
                () -> assertTrue(config.contains("LoggingEventCompositeJsonEncoder"))
        );
    }
}
