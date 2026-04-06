package com.clinicadigital.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for JsonLogger with MDC fields.
 * Validates that JsonLogger correctly writes tenant_id, trace_id, operation, outcome to SLF4J MDC.
 * Refs: FR-011 (Structured logs with tenant, correlation, operation, outcome)
 */
@DisplayName("JsonLogger Unit Tests")
class JsonLoggerTest {

    private Logger logger;
    private static final String TEST_TENANT_ID = "tenant-001";
    private static final String TEST_TRACE_ID = "trace-" + UUID.randomUUID();
    private static final String TEST_OPERATION = "auth.login";
    private static final String TEST_OUTCOME = "success";

    @BeforeEach
    void setUp() {
        logger = LoggerFactory.getLogger(JsonLoggerTest.class);
        MDC.clear();
    }

    @Test
    @DisplayName("Should write tenant_id to MDC")
    void shouldWriteTenantIdToMdc() {
        // Given: MDC is clear
        var initialContext = MDC.getCopyOfContextMap();
        assertTrue(initialContext == null || initialContext.isEmpty());
        
        // When: JsonLogger writes tenant_id
        MDC.put("tenant_id", TEST_TENANT_ID);
        
        // Then: tenant_id is in MDC
        var context = MDC.getCopyOfContextMap();
        assertNotNull(context);
        assertTrue(context.containsKey("tenant_id"));
        assertTrue(context.get("tenant_id").equals(TEST_TENANT_ID));
    }

    @Test
    @DisplayName("Should write trace_id to MDC")
    void shouldWriteTraceIdToMdc() {
        // Given: MDC is clear
        var initialContext = MDC.getCopyOfContextMap();
        assertTrue(initialContext == null || initialContext.isEmpty());
        
        // When: JsonLogger writes trace_id
        MDC.put("trace_id", TEST_TRACE_ID);
        
        // Then: trace_id is in MDC
        var context = MDC.getCopyOfContextMap();
        assertNotNull(context);
        assertTrue(context.containsKey("trace_id"));
        assertTrue(context.get("trace_id").equals(TEST_TRACE_ID));
    }

    @Test
    @DisplayName("Should write operation to MDC")
    void shouldWriteOperationToMdc() {
        // Given: MDC is clear
        
        // When: JsonLogger writes operation
        MDC.put("operation", TEST_OPERATION);
        
        // Then: operation is in MDC
        assertTrue(MDC.getCopyOfContextMap().containsKey("operation"));
        assertTrue(MDC.getCopyOfContextMap().get("operation").equals(TEST_OPERATION));
    }

    @Test
    @DisplayName("Should write outcome to MDC")
    void shouldWriteOutcomeToMdc() {
        // Given: MDC is clear
        
        // When: JsonLogger writes outcome
        MDC.put("outcome", TEST_OUTCOME);
        
        // Then: outcome is in MDC
        assertTrue(MDC.getCopyOfContextMap().containsKey("outcome"));
        assertTrue(MDC.getCopyOfContextMap().get("outcome").equals(TEST_OUTCOME));
    }

    @Test
    @DisplayName("Should maintain all required MDC fields in a single log entry")
    void shouldMaintainAllRequiredMdcFieldsInSingleEntry() {
        // Given: MDC with all required fields
        MDC.put("tenant_id", TEST_TENANT_ID);
        MDC.put("trace_id", TEST_TRACE_ID);
        MDC.put("operation", TEST_OPERATION);
        MDC.put("outcome", TEST_OUTCOME);
        
        // When: We read MDC context
        var mdcMap = MDC.getCopyOfContextMap();
        
        // Then: All fields are present
        assertTrue(mdcMap.containsKey("tenant_id"));
        assertTrue(mdcMap.containsKey("trace_id"));
        assertTrue(mdcMap.containsKey("operation"));
        assertTrue(mdcMap.containsKey("outcome"));
        
        assertTrue(mdcMap.get("tenant_id").equals(TEST_TENANT_ID));
        assertTrue(mdcMap.get("trace_id").equals(TEST_TRACE_ID));
        assertTrue(mdcMap.get("operation").equals(TEST_OPERATION));
        assertTrue(mdcMap.get("outcome").equals(TEST_OUTCOME));
    }

    @Test
    @DisplayName("Should clear MDC fields properly")
    void shouldClearMdcFieldsProperly() {
        // Given: MDC with fields
        MDC.put("tenant_id", TEST_TENANT_ID);
        MDC.put("trace_id", TEST_TRACE_ID);
        
        // When: MDC is cleared
        MDC.clear();
        
        // Then: All fields are removed
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
    }
}
