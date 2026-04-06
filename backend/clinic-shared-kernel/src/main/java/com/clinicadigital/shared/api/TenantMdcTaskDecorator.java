package com.clinicadigital.shared.api;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Spring {@link TaskDecorator} that propagates {@link TenantContext} and MDC state
 * from the calling thread to the async execution thread, and restores the original
 * state after execution (FR-002b).
 *
 * <p>Registration: configure as the default decorator in {@code AsyncConfigurer}
 * inside the gateway application:
 * <pre>{@code
 * executor.setTaskDecorator(new TenantMdcTaskDecorator());
 * }</pre>
 *
 * <p>Fail-closed: if the calling thread has no {@link TenantContextStore} value,
 * decoration fails immediately (FR-002b, FR-002a). Async tasks submitted without
 * a propagated tenant context are rejected before any work executes.
 *
 * <p>Thread safety: stateless; each call to {@link #decorate} captures a new
 * snapshot — safe for concurrent use across multiple threads and task submissions.
 */
public class TenantMdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        TenantContext capturedCtx = TenantContextStore.get();
        if (capturedCtx == null) {
            throw new IllegalStateException(
                    "TenantMdcTaskDecorator: no TenantContext available in calling thread. " +
                    "Async tasks must be submitted from a context where a tenant is " +
                    "initialised (FR-002b). Fail-closed per FR-002a.");
        }
        // Snapshot MDC before thread handoff — MDC is thread-local and does not
        // cross thread boundaries automatically.
        Map<String, String> capturedMdc = MDC.getCopyOfContextMap();

        return () -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            TenantContext previousCtx = TenantContextStore.get();
            try {
                TenantContextStore.set(capturedCtx);
                if (capturedMdc != null) {
                    MDC.setContextMap(capturedMdc);
                } else {
                    MDC.clear();
                }
                runnable.run();
            } finally {
                // Always restore — prevents context leakage under thread pool reuse.
                if (previousMdc != null) {
                    MDC.setContextMap(previousMdc);
                } else {
                    MDC.clear();
                }
                if (previousCtx != null) {
                    TenantContextStore.set(previousCtx);
                } else {
                    TenantContextStore.clear();
                }
            }
        };
    }
}
