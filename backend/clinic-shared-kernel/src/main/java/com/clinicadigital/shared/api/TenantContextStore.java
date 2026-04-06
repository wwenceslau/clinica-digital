package com.clinicadigital.shared.api;

import com.clinicadigital.shared.api.TenantContext;

/**
 * Thread-local store for TenantContext used in async execution paths
 * where Spring RequestScope is not active (@Async, CompletableFuture, @Scheduled).
 *
 * <p>Purpose: The Spring-scoped {@link TenantContextHolder} bean is not accessible
 * outside the HTTP request thread. This store provides a plain ThreadLocal-backed
 * fallback that the {@link TenantMdcTaskDecorator} populates before handing off
 * work to a pool thread.
 *
 * <p>Lifecycle: MUST be cleared (via {@link #clear()}) by the request lifecycle
 * filter or async task finally block to prevent context leakage under thread
 * pool reuse (FR-002b, TenantContextCleanupTest).
 */
public final class TenantContextStore {

    private static final ThreadLocal<TenantContext> STORE = new ThreadLocal<>();

    private TenantContextStore() {}

    public static void set(TenantContext ctx) {
        STORE.set(ctx);
    }

    public static TenantContext get() {
        return STORE.get();
    }

    public static void clear() {
        STORE.remove();
    }
}
