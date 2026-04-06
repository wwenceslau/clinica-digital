package com.clinicadigital.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.slf4j.MDC;

import java.util.concurrent.Callable;

/**
 * TenantAwareMeterRegistry wraps Micrometer MeterRegistry with automatic tenant_id tagging.
 * 
 * Ensures that all metrics are automatically tagged with:
 * - tenant_id: The current tenant context from MDC
 * - trace_id: Optional trace ID for metric correlation
 * 
 * Benefits:
 * - Transparent tenant isolation at metrics layer
 * - Automatic per-tenant dashboards and alerts
 * - Prevents metric aggregation confusion across tenants
 * 
 * Refs: FR-010a (Tenant-aware metrics with Micrometer tags)
 */
public class TenantAwareMeterRegistry {

    private final MeterRegistry delegate;

    /**
     * Creates a TenantAwareMeterRegistry wrapping the provided delegate registry.
     * 
     * @param delegate The underlying Micrometer MeterRegistry
     */
    public TenantAwareMeterRegistry(MeterRegistry delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate MeterRegistry cannot be null");
        }
        this.delegate = delegate;
    }

    /**
     * Gets the current tenant ID from MDC for tagging.
     * 
     * @return The tenant_id from MDC, or "unknown" if not set (fail-safe)
     */
    private String getCurrentTenantId() {
        String tenantId = MDC.get("tenant_id");
        return tenantId != null && !tenantId.isEmpty() ? tenantId : "unknown";
    }

    /**
     * Gets the current trace ID from MDC for optional metrics correlation.
     * 
     * @return The trace_id from MDC, or null if not set
     */
    private String getCurrentTraceId() {
        return MDC.get("trace_id");
    }

    /**
     * Gets tenant-aware tags that include tenant_id and optionally trace_id.
     * 
     * @return Tags containing tenant_id and trace_id (if available)
     */
    private Tags getTenantAwareTags(String... additionalTags) {
        Tags tags = Tags.of("tenant_id", getCurrentTenantId());
        
        String traceId = getCurrentTraceId();
        if (traceId != null && !traceId.isEmpty()) {
            tags = tags.and("trace_id", traceId);
        }
        
        // Add any additional tags provided
        if (additionalTags != null && additionalTags.length > 0) {
            for (int i = 0; i < additionalTags.length; i += 2) {
                if (i + 1 < additionalTags.length) {
                    tags = tags.and(additionalTags[i], additionalTags[i + 1]);
                }
            }
        }
        
        return tags;
    }

    /**
     * Records a timer with automatic tenant tagging.
     * 
     * @param name The metric name
     * @param tags Additional tags (varargs, key-value pairs)
     * @return The Timer for recording durations
     */
    public Timer timer(String name, String... tags) {
        return delegate.timer(name, getTenantAwareTags(tags));
    }

    /**
     * Records a counter with automatic tenant tagging.
     * 
     * @param name The metric name
     * @param tags Additional tags (varargs, key-value pairs)
     * @return The Counter for incrementing counts
     */
    public Counter counter(String name, String... tags) {
        return delegate.counter(name, getTenantAwareTags(tags));
    }

    /**
     * Times a callable operation with automatic tenant tagging.
     * 
     * @param <T> The return type of the callable
     * @param name The metric name
     * @param callable The operation to time
     * @param tags Additional tags (varargs, key-value pairs)
     * @return The result of the callable
     * @throws Exception if the callable throws
     */
    public <T> T recordCallableWithTenant(String name, Callable<T> callable, String... tags) throws Exception {
        Timer timer = timer(name, tags);
        return timer.recordCallable(callable);
    }

    /**
     * Times a runnable operation with automatic tenant tagging.
     * 
     * @param name The metric name
     * @param runnable The operation to time
     * @param tags Additional tags (varargs, key-value pairs)
     */
    public void recordRunnableWithTenant(String name, Runnable runnable, String... tags) {
        Timer timer = timer(name, tags);
        timer.record(runnable);
    }

    /**
     * Records a failed operation count with tenant tagging.
     * 
     * @param operationName Name of the failed operation
     * @param exceptionType Type of exception
     * @param tags Additional tags (varargs, key-value pairs)
     */
    public void recordFailure(String operationName, String exceptionType, String... tags) {
        String[] allTags = new String[tags.length + 2];
        System.arraycopy(tags, 0, allTags, 0, tags.length);
        allTags[tags.length] = "exception_type";
        allTags[tags.length + 1] = exceptionType;
        
        counter(operationName + ".failures", allTags).increment();
    }

    /**
     * Records a successful operation count with tenant tagging.
     * 
     * @param operationName Name of the successful operation
     * @param tags Additional tags (varargs, key-value pairs)
     */
    public void recordSuccess(String operationName, String... tags) {
        counter(operationName + ".successes", tags).increment();
    }

    /**
     * Records quota exceeded event for a tenant.
     * 
     * @param quotaName Name of the quota (e.g., "requests_per_minute")
     */
    public void recordQuotaExceeded(String quotaName) {
        counter("quota.exceeded", "quota_name", quotaName).increment();
    }

    /**
     * Returns the underlying MeterRegistry for direct access if needed.
     * 
     * @return The wrapped MeterRegistry
     */
    public MeterRegistry getDelegate() {
        return delegate;
    }
}
