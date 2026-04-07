package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an authenticated session scoped to a single tenant.
 * Refs: FR-007, FR-010a, Art. 0
 */
@Entity
@Table(name = "iam_sessions")
public class IamSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "trace_id")
    private String traceId;

    protected IamSession() {
        // JPA constructor
    }

    public IamSession(
            UUID id,
            UUID tenantId,
            UUID userId,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt,
            String traceId,
            String clientIp,
            String userAgent) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.traceId = traceId;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    public IamSession(
            UUID id,
            UUID tenantId,
            UUID userId,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt,
            String traceId) {
        this(id, tenantId, userId, issuedAt, expiresAt, revokedAt, traceId, null, null);
    }

    /**
     * Returns true when the session is neither expired nor revoked.
     */
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (issuedAt == null) {
            issuedAt = now;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID userId() {
        return userId;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public String clientIp() {
        return clientIp;
    }

    public String userAgent() {
        return userAgent;
    }

    public String traceId() {
        return traceId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IamSession that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
