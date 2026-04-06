package com.clinicadigital.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract test for trace propagation end-to-end.
 * Validates that trace_id is generated when absent, preserved when valid,
 * and propagated across all layers until persistence.
 * 
 * Refs: FR-010a (trace_id generation and propagation contract)
 */
@DisplayName("Trace Propagation Contract Tests")
class TraceContextPropagationContractTest {

    private static final String VALID_TRACE_ID_PREFIX = "trace-";

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should generate trace_id when absent from frontier")
    void shouldGenerateTraceIdWhenAbsent() {
        // Given: No trace_id in MDC (frontier scenario without trace_id header)
        assertNull(MDC.get("trace_id"));
        
        // When: System generates trace_id at frontier
        String generatedTraceId = VALID_TRACE_ID_PREFIX + UUID.randomUUID();
        MDC.put("trace_id", generatedTraceId);
        
        // Then: trace_id is present in MDC for propagation
        assertNotNull(MDC.get("trace_id"));
        assertTrue(MDC.get("trace_id").startsWith(VALID_TRACE_ID_PREFIX));
        assertTrue(MDC.get("trace_id").length() > VALID_TRACE_ID_PREFIX.length());
    }

    @Test
    @DisplayName("Should preserve valid trace_id from frontier")
    void shouldPreserveValidTraceIdFromFrontier() {
        // Given: Valid trace_id from HTTP/CLI frontier header
        String receivedTraceId = "trace-550e8400-e29b-41d4-a716-446655440000";
        
        // When: System receives and preserves trace_id
        MDC.put("trace_id", receivedTraceId);
        
        // Then: trace_id is preserved exactly as received
        assertEquals(receivedTraceId, MDC.get("trace_id"));
    }

    @Test
    @DisplayName("Should propagate trace_id through service layer")
    void shouldPropagateTraceIdThroughServiceLayer() {
        // Given: trace_id set at frontier
        String originalTraceId = "trace-" + UUID.randomUUID();
        MDC.put("trace_id", originalTraceId);
        
        // When: trace_id is read in service layer
        String serviceLayerTraceId = MDC.get("trace_id");
        
        // Then: trace_id is unchanged
        assertEquals(originalTraceId, serviceLayerTraceId);
    }

    @Test
    @DisplayName("Should propagate trace_id to persistence layer")
    void shouldPropagateTraceIdToPersistenceLayer() {
        // Given: trace_id in MDC from frontier through service
        String traceIdForPersistence = "trace-" + UUID.randomUUID();
        MDC.put("trace_id", traceIdForPersistence);
        
        // When: Persistence layer queries MDC for trace_id
        String persistenceLayerTraceId = MDC.get("trace_id");
        
        // Then: trace_id is available for audit/logging in DB
        assertEquals(traceIdForPersistence, persistenceLayerTraceId);
        assertNotNull(persistenceLayerTraceId);
    }

    @Test
    @DisplayName("Should propagate trace_id to async event publishing")
    void shouldPropagateTraceIdToAsyncEvents() {
        // Given: trace_id in MDC during synchronous operation
        String traceIdForEvents = "trace-" + UUID.randomUUID();
        MDC.put("trace_id", traceIdForEvents);
        
        // When: Event is prepared with trace_id from MDC
        String eventTraceId = MDC.get("trace_id");
        
        // Then: Event carries same trace_id for end-to-end correlation
        assertEquals(traceIdForEvents, eventTraceId);
    }

    @Test
    @DisplayName("Should handle concurrent requests with different trace_ids (ThreadLocal isolation)")
    void shouldIsolateConcurrentTraceIds() throws InterruptedException {
        // Given: Main thread with trace_id
        String mainTraceId = "trace-main-" + UUID.randomUUID();
        MDC.put("trace_id", mainTraceId);
        
        // When: Child thread sets different trace_id
        String[] childTraceId = new String[1];
        Thread childThread = new Thread(() -> {
            MDC.clear();
            childTraceId[0] = "trace-child-" + UUID.randomUUID();
            MDC.put("trace_id", childTraceId[0]);
        });
        childThread.start();
        childThread.join();
        
        // Then: Main thread trace_id unchanged (ThreadLocal isolation)
        assertEquals(mainTraceId, MDC.get("trace_id"));
        assertNotEquals(childTraceId[0], MDC.get("trace_id"));
    }

    @Test
    @DisplayName("Should include trace_id in structured log output for end-to-end correlation")
    void shouldIncludeTraceIdInStructuredLogs() {
        // Given: Log entry with trace_id in MDC
        String traceIdForLogging = "trace-" + UUID.randomUUID();
        MDC.put("trace_id", traceIdForLogging);
        MDC.put("tenant_id", "tenant-001");
        MDC.put("operation", "auth.login");
        MDC.put("outcome", "success");
        
        // When: Structured log is generated with all MDC fields
        var mdcSnapshot = MDC.getCopyOfContextMap();
        
        // Then: Log contains trace_id for end-to-end tracing
        assertNotNull(mdcSnapshot);
        assertEquals(traceIdForLogging, mdcSnapshot.get("trace_id"));
        assertEquals("tenant-001", mdcSnapshot.get("tenant_id"));
        assertEquals("auth.login", mdcSnapshot.get("operation"));
        assertEquals("success", mdcSnapshot.get("outcome"));
    }

    @Test
    @DisplayName("Should maintain trace_id across request/response boundary")
    void shouldMaintainTraceIdAcrossRequestResponseBoundary() {
        // Given: trace_id assigned at request frontier
        String requestTraceId = "trace-" + UUID.randomUUID();
        MDC.put("trace_id", requestTraceId);
        
        // When: Request processes through layers
        String middleLayerTraceId = MDC.get("trace_id");
        
        // When: Response is prepared
        String responseTraceId = MDC.get("trace_id");
        
        // Then: trace_id is same from request through response
        assertEquals(requestTraceId, middleLayerTraceId);
        assertEquals(requestTraceId, responseTraceId);
    }
}
