package com.clinicadigital.shared.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceContextTest {

    @Test
    void shouldGenerateNewTraceIdentifier() {
        TraceContext first = TraceContext.generate();
        TraceContext second = TraceContext.generate();

        assertTrue(first.traceId().length() >= 32);
        assertNotEquals(first.traceId(), second.traceId());
    }

    @Test
    void shouldPreserveProvidedTraceIdentifier() {
        TraceContext context = TraceContext.from("trace-123");

        assertEquals("trace-123", context.traceId());
    }

    @Test
    void shouldRejectBlankTraceIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> TraceContext.from(" "));
    }
}