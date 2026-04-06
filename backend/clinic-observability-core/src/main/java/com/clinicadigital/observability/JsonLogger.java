package com.clinicadigital.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * JsonLogger encapsulates structured logging with mandatory MDC fields.
 * 
 * Ensures that all logs include:
 * - tenant_id: Identifies the tenant context (required)
 * - trace_id: Unique identifier for end-to-end tracing (required, auto-generated if missing)
 * - operation: Name of the operation being performed (required)
 * - outcome: Result of the operation (required, success|failure|partial)
 * 
 * Refs: FR-010, FR-011 (Structured observability with tenant/trace correlation)
 */
public class JsonLogger {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLogger.class);
    private static final String TRACE_ID_PREFIX = "trace-";
    
    private final String tenantId;
    private final String traceId;

    /**
     * Creates a JsonLogger with explicit tenant_id and auto-generated trace_id if needed.
     * 
     * @param tenantId The tenant context (required, non-null)
     * @throws IllegalArgumentException if tenantId is null or empty
     */
    public JsonLogger(String tenantId) {
        this(tenantId, null);
    }

    /**
     * Creates a JsonLogger with explicit tenant_id and trace_id.
     * 
     * @param tenantId The tenant context (required, non-null)
     * @param traceId The trace ID for correlation (if null, generates new UUID-based trace_id)
     * @throws IllegalArgumentException if tenantId is null or empty
     */
    public JsonLogger(String tenantId, String traceId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("tenantId cannot be null or empty");
        }
        this.tenantId = tenantId;
        this.traceId = traceId != null && !traceId.isEmpty() 
            ? traceId 
            : TRACE_ID_PREFIX + UUID.randomUUID();
        
        // Initialize MDC with tenant and trace context
        this.initializeMdc();
    }

    /**
     * Initializes MDC with tenant_id and trace_id context.
     * Must be called once at the start of a request/operation.
     */
    private void initializeMdc() {
        MDC.put("tenant_id", this.tenantId);
        MDC.put("trace_id", this.traceId);
    }

    /**
     * Logs an operation start event with INFO level.
     * 
     * @param operation Name of the operation (e.g., "auth.login", "tenant.create")
     * @param message Additional context message
     */
    public void logOperationStart(String operation, String message) {
        MDC.put("operation", operation);
        MDC.put("outcome", "started");
        LOGGER.info("Operation started: {} - {}", operation, message);
    }

    /**
     * Logs a successful operation completion.
     * 
     * @param operation Name of the operation
     * @param message Success details
     */
    public void logSuccess(String operation, String message) {
        MDC.put("operation", operation);
        MDC.put("outcome", "success");
        LOGGER.info("Operation succeeded: {} - {}", operation, message);
    }

    /**
     * Logs an operation failure with error context.
     * 
     * @param operation Name of the operation
     * @param message Failure details
     * @param exception The exception that caused the failure
     */
    public void logFailure(String operation, String message, Throwable exception) {
        MDC.put("operation", operation);
        MDC.put("outcome", "failure");
        LOGGER.error("Operation failed: {} - {} - Error: {}", operation, message, exception.getMessage(), exception);
    }

    /**
     * Logs a partial completion (some sub-operations succeeded, others failed).
     * 
     * @param operation Name of the operation
     * @param message Partial completion details
     */
    public void logPartial(String operation, String message) {
        MDC.put("operation", operation);
        MDC.put("outcome", "partial");
        LOGGER.warn("Operation partial: {} - {}", operation, message);
    }

    /**
     * Logs a security-relevant event (e.g., access denial, quota exceeded).
     * 
     * @param operation Name of the operation
     * @param message Security event details
     */
    public void logSecurityEvent(String operation, String message) {
        MDC.put("operation", operation);
        MDC.put("outcome", "security_event");
        LOGGER.warn("Security event - Operation: {} - {}", operation, message);
    }

    /**
     * Gets the current trace_id for propagation to downstream systems (events, async tasks).
     * 
     * @return The trace ID
     */
    public String getTraceId() {
        return this.traceId;
    }

    /**
     * Gets the current tenant_id.
     * 
     * @return The tenant ID
     */
    public String getTenantId() {
        return this.tenantId;
    }

    /**
     * Clears MDC context (important for ThreadLocal cleanup after request completes).
     * Should be called in a finally block or request filter cleanup.
     */
    public static void clearContext() {
        MDC.clear();
    }

    /**
     * Returns current MDC context snapshot for debugging or event enrichment.
     * 
     * @return Copy of current MDC context
     */
    public static java.util.Map<String, String> getCurrentContext() {
        return MDC.getCopyOfContextMap();
    }
}
