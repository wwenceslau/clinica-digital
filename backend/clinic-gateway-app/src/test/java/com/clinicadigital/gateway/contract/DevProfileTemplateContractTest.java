package com.clinicadigital.gateway.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevProfileTemplateContractTest {

    @Test
    void shouldKeepDatabaseSecretsExternalizedInDevTemplate() throws IOException {
        String content = Files.readString(Path.of("src/main/resources/application-dev.yml.template"));

        assertAll(
                () -> assertTrue(content.contains("${DB_URL}")),
                () -> assertTrue(content.contains("${DB_USER}")),
                () -> assertTrue(content.contains("${DB_PASSWORD}")),
                () -> assertTrue(content.contains("spring:"))
        );
    }
}
