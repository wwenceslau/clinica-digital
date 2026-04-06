package com.clinicadigital.shared.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationOutcomeTest {

    @Test
    void shouldBuildOutcomeWithIssuesAndSeverities() {
        OperationOutcome outcome = OperationOutcome.builder()
                .addIssue(OperationOutcome.Severity.ERROR, "forbidden", "tenant context missing")
                .addIssue(OperationOutcome.Severity.WARNING, "processing", "fallback applied")
                .build();

        assertEquals(2, outcome.issue().size());
        assertEquals(OperationOutcome.Severity.ERROR, outcome.issue().getFirst().severity());
        assertEquals("forbidden", outcome.issue().getFirst().code());
        assertEquals("tenant context missing", outcome.issue().getFirst().diagnostics());
    }

    @Test
    void shouldRejectEmptyOutcomeBuild() {
        assertThrows(IllegalStateException.class, () -> OperationOutcome.builder().build());
    }
}