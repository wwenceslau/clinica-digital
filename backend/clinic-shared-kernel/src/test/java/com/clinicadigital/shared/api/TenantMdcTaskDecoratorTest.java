package com.clinicadigital.shared.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T030e - Unit tests for TenantMdcTaskDecorator.
 * Written BEFORE implementation per Art. I (Test-First).
 * Refs: FR-002b, FR-010a
 */
class TenantMdcTaskDecoratorTest {

    private TenantMdcTaskDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new TenantMdcTaskDecorator();
        MDC.clear();
        TenantContextStore.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        TenantContextStore.clear();
    }

    @Test
    void shouldPropagateTenantContextToDecoratedRunnable() {
        UUID tenantId = UUID.randomUUID();
        TenantContextStore.set(TenantContext.from(tenantId));

        AtomicReference<TenantContext> observed = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> observed.set(TenantContextStore.get()));
        decorated.run();

        assertNotNull(observed.get(), "TenantContext must be propagated to the decorated runnable");
        assertEquals(tenantId, observed.get().tenantId());
    }

    @Test
    void shouldPropagateMdcSnapshotToDecoratedRunnable() {
        TenantContextStore.set(TenantContext.from(UUID.randomUUID()));
        MDC.put("trace_id", "test-trace-123");
        MDC.put("tenant_id", "test-tenant");

        AtomicReference<Map<String, String>> observedMdc = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> observedMdc.set(MDC.getCopyOfContextMap()));
        decorated.run();

        assertNotNull(observedMdc.get(), "MDC snapshot must be propagated");
        assertEquals("test-trace-123", observedMdc.get().get("trace_id"));
        assertEquals("test-tenant", observedMdc.get().get("tenant_id"));
    }

    @Test
    void shouldRestoreOriginalMdcAfterExecution() {
        TenantContextStore.set(TenantContext.from(UUID.randomUUID()));
        MDC.put("trace_id", "original-trace");

        // Decoration captures current MDC ("original-trace")
        Runnable decorated = decorator.decorate(() -> MDC.put("trace_id", "async-trace"));
        decorated.run();

        // Caller thread MDC must be restored after run
        assertEquals("original-trace", MDC.get("trace_id"),
                "MDC must be restored to pre-run state after decorated task completes");
    }

    @Test
    void shouldRestoreOriginalTenantContextStoreAfterExecution() {
        UUID callerTenantId = UUID.randomUUID();
        UUID capturedTenantId = UUID.randomUUID();

        // Context when decoration is called
        TenantContextStore.set(TenantContext.from(capturedTenantId));
        Runnable decorated = decorator.decorate(() -> {});

        // Pool thread has its own context
        TenantContextStore.set(TenantContext.from(callerTenantId));
        decorated.run();

        // Pool thread context must be restored after run
        assertEquals(callerTenantId, TenantContextStore.get().tenantId(),
                "TenantContextStore must be restored to pool-thread value after decorated task");
    }

    @Test
    void shouldFailClosedWhenNoTenantContextInCallingThread() {
        // No tenant context set in calling thread — fail-closed per FR-002b
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> decorator.decorate(() -> {})
        );
        assertTrue(ex.getMessage().contains("TenantMdcTaskDecorator"),
                "Exception must mention TenantMdcTaskDecorator, was: " + ex.getMessage());
    }

    @Test
    void shouldClearTenantContextStoreWhenPoolThreadHadNoContextBeforeRun() {
        UUID capturedTenantId = UUID.randomUUID();
        TenantContextStore.set(TenantContext.from(capturedTenantId));
        Runnable decorated = decorator.decorate(() -> {});

        // Simulate pool thread with no prior context
        TenantContextStore.clear();
        decorated.run();

        assertNull(TenantContextStore.get(),
                "TenantContextStore must be null after run when pool thread had no prior context");
    }
}
