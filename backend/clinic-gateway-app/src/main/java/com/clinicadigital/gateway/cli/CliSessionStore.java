package com.clinicadigital.gateway.cli;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * T109 [US8] Holds the active CLI session state between commands.
 *
 * <p>When {@code login} or {@code select-organization} succeeds, it stores the
 * opaque session token (UUID) and the tenant UUID in this singleton bean.
 * The {@code logout} command then reads from here, revokes the session, and clears the store.
 *
 * <p>The session token is <strong>never printed</strong> in CLI output per the CLI contract:
 * "O token opaco é persistido prioritariamente em cookie seguro em produção; o CLI apenas
 * reflete o resultado da emissão/invalidação da sessão, sem imprimir segredos persistidos em log."
 *
 * Refs: FR-010, FR-024
 */
@Component
public class CliSessionStore {

    private volatile UUID sessionId;
    private volatile UUID tenantId;

    /** Store the session obtained from a successful login or select-organization. */
    public synchronized void store(UUID sessionId, UUID tenantId) {
        this.sessionId = sessionId;
        this.tenantId = tenantId;
    }

    public synchronized UUID getSessionId() {
        return sessionId;
    }

    public synchronized UUID getTenantId() {
        return tenantId;
    }

    public synchronized boolean hasSession() {
        return sessionId != null && tenantId != null;
    }

    /** Clear the stored session (called after successful logout). */
    public synchronized void clear() {
        sessionId = null;
        tenantId = null;
    }
}
