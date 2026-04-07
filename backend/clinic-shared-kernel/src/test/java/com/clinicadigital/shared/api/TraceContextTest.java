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

        assertTrue(first.traceId().startsWith("trace-"));
        assertTrue(TraceContext.isValid(first.traceId()));
        assertNotEquals(first.traceId(), second.traceId());
    }

    @Test
    void shouldPreserveProvidedTraceIdentifier() {
        TraceContext context = TraceContext.from("trace-123");

        assertEquals("trace-123", context.traceId());
    }

    @Test
    void shouldAcceptOnlyCanonicalGeneratedTraceFormatAsValid() {
        assertTrue(TraceContext.isValid("trace-550e8400-e29b-41d4-a716-446655440000"));
        assertTrue(TraceContext.isValid(TraceContext.generate().traceId()));
        assertTrue(!TraceContext.isValid("550e8400-e29b-41d4-a716-446655440000"));
        assertTrue(!TraceContext.isValid("trace-123"));
    }

    @Test
    void shouldRejectBlankTraceIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> TraceContext.from(" "));
    }
}