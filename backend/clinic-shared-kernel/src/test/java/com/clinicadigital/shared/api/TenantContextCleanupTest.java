package com.clinicadigital.shared.api;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T030g - Unit tests verifying that TenantContextHolder, TraceContextHolder,
 * TenantContextStore, and MDC are properly cleared at end of request/CLI
 * invocation to prevent context leakage under thread pool reuse.
 * Refs: FR-002b, thread-pool safety
 */
class TenantContextCleanupTest {

    @Test
    void tenantContextHolderShouldBeAbsentAfterClear() {
        TenantContextHolder holder = new TenantContextHolder();
        holder.set(TenantContext.from(UUID.randomUUID()));
        assertTrue(holder.isPresent(), "Pre-condition: context must be set");

        holder.clear();

        assertFalse(holder.isPresent(),
                "TenantContextHolder.clear() must remove context to prevent thread-pool leak");
    }

    @Test
    void tenantContextHolderGetRequiredShouldThrowAfterClear() {
        TenantContextHolder holder = new TenantContextHolder();
        holder.set(TenantContext.from(UUID.randomUUID()));
        holder.clear();

        assertThrows(IllegalStateException.class, holder::getRequired,
                "getRequired() after clear() must throw, not return stale context");
    }

    @Test
    void traceContextHolderShouldBeAbsentAfterClear() {
        TraceContextHolder holder = new TraceContextHolder();
        holder.set(TraceContext.generate());
        assertTrue(holder.isPresent(), "Pre-condition: trace context must be set");

        holder.clear();

        assertFalse(holder.isPresent(),
                "TraceContextHolder.clear() must remove context to prevent thread-pool leak");
    }

    @Test
    void mdcShouldBeEmptyAfterClear() {
        MDC.put("tenant_id", "some-tenant");
        MDC.put("trace_id", "some-trace");
        MDC.put("operation", "login");

        // Simulates what a request lifecycle filter must call in its finally block
        MDC.clear();

        assertNull(MDC.get("tenant_id"), "tenant_id must be cleared from MDC after request");
        assertNull(MDC.get("trace_id"), "trace_id must be cleared from MDC after request");
        assertNull(MDC.get("operation"), "operation must be cleared from MDC after request");
    }

    @Test
    void tenantContextStoreShouldBeNullAfterClear() {
        TenantContextStore.set(TenantContext.from(UUID.randomUUID()));
        assertNotNull(TenantContextStore.get(), "Pre-condition: store must hold a value");

        TenantContextStore.clear();

        assertNull(TenantContextStore.get(),
                "TenantContextStore.clear() must remove ThreadLocal value to prevent async thread-pool leak");
    }

    @Test
    void multipleConsecutiveClearsShouldBeIdempotent() {
        TenantContextHolder holder = new TenantContextHolder();
        // Clear on empty holder must not throw
        assertDoesNotThrow(() -> {
            holder.clear();
            holder.clear();
        });
        assertFalse(holder.isPresent());
    }
}
