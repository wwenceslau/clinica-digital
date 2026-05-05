package com.clinicadigital.shared.api;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

/**
 * Executes {@code SET LOCAL app.tenant_id = '<uuid>'} on the active JDBC connection
 * before any tenant-scoped query runs.
 *
 * <p>This component satisfies FR-016 (SET LOCAL is mandatory at every entry boundary:
 * HTTP, CLI, async consumers, scheduled tasks) and FR-016a (fail-closed when no
 * TenantContext is available in the calling thread).
 *
 * <p>Must be called <strong>within an open transaction</strong> because {@code SET LOCAL}
 * is transaction-scoped and is automatically reset on commit or rollback.
 *
 * <p>Thread safety: stateless; safe for concurrent use.
 */
@Component
public class TenantJdbcContextInterceptor {

    private final JdbcOperations jdbc;
    private final TenantContextHolder tenantContextHolder;

    public TenantJdbcContextInterceptor(JdbcOperations jdbc,
                                        TenantContextHolder tenantContextHolder) {
        this.jdbc = jdbc;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Sets the PostgreSQL session-local {@code app.crosslogin = 'true'} to
     * enable the US4 cross-tenant login RLS policies.
     *
     * <p>Must be called <strong>within an open transaction</strong> because
     * {@code set_config} with {@code is_local = true} (equivalent to SET LOCAL)
     * is transaction-scoped and is automatically reset on commit or rollback.
     *
     * <p>This grants SELECT access on {@code iam_users} (by email),
     * full DML on {@code iam_auth_challenges}, INSERT on {@code iam_sessions},
     * and INSERT on {@code iam_audit_events} with null tenant_id.
     */
    public void applyLoginContext() {
        // set_config(key, value, is_local) — is_local=true equals SET LOCAL
        jdbc.execute("SELECT set_config('app.crosslogin', 'true', true)");
    }

    /**
     * Sets the PostgreSQL session-local {@code app.tenant_id} to the UUID held by
     * {@link TenantContextHolder} (HTTP requests) or {@link TenantContextStore}
     * (CLI, async, and scheduled contexts where request scope is not active).
     *
     * <p>Fails immediately with {@link IllegalStateException} if no tenant context
     * is present in either store (fail-closed per FR-016a). The {@code SET LOCAL}
     * statement uses the UUID string literal directly; {@code UUID.toString()} produces
     * only hex digits and hyphens, which are safe from SQL injection.
     *
     * @throws IllegalStateException if neither {@link TenantContextHolder} nor
     *         {@link TenantContextStore} has an active context
     */
    public void applyTenantContext() {
        // Primary path: request-scoped TenantContextHolder (HTTP request context).
        // In CLI / async / scheduled contexts the request scope proxy is not active,
        // so we catch the resulting RuntimeException and fall through to TenantContextStore.
        try {
            if (tenantContextHolder.isPresent()) {
                String tenantId = tenantContextHolder.getRequired().tenantId().toString();
                jdbc.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");
                return;
            }
        } catch (RuntimeException scopeException) {
            // Request scope not active (CLI, @Async, @Scheduled) — fall through.
        }

        // Fallback path: thread-local TenantContextStore (CLI, async, scheduled).
        TenantContext storedCtx = TenantContextStore.get();
        if (storedCtx != null) {
            // UUID.toString() is injection-safe (hex + hyphens only).
            jdbc.execute("SET LOCAL app.tenant_id = '" + storedCtx.tenantId() + "'");
            return;
        }

        throw new IllegalStateException(
                "Cannot initialise JDBC tenant context: TenantContextHolder is empty. " +
                "Fail-closed violation detected (FR-016a). Every tenant-scoped " +
                "operation MUST have an initialised TenantContext before reaching " +
                "the persistence layer.");
    }
}
