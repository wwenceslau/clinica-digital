package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "iam_user_id", nullable = false)
    private UUID userId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

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

    @Column(name = "opaque_token_digest")
    private String opaqueTokenDigest;

    @Column(name = "revocation_reason")
    private String revocationReason;

    @Column(name = "active_practitioner_role_id")
    private UUID activePractitionerRoleId;

    @Transient
    private UUID opaqueToken;

    protected IamSession() {
        // JPA constructor
    }

    public IamSession(
            UUID id,
            UUID tenantId,
            UUID organizationId,
            UUID userId,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt,
            String traceId,
            String opaqueTokenDigest,
            String clientIp,
            String userAgent) {
        this.id = id;
        this.tenantId = tenantId;
        this.organizationId = organizationId;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.traceId = traceId;
        this.opaqueTokenDigest = opaqueTokenDigest;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    public IamSession(
            UUID id,
            UUID tenantId,
            UUID organizationId,
            UUID userId,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt,
            String traceId,
            String opaqueTokenDigest) {
        this(id, tenantId, organizationId, userId, issuedAt, expiresAt, revokedAt, traceId, opaqueTokenDigest, null, null);
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
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public boolean activeFlag() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID organizationId() {
        return organizationId;
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

    public String opaqueTokenDigest() {
        return opaqueTokenDigest;
    }

    public String revocationReason() {
        return revocationReason;
    }

    public UUID opaqueToken() {
        return opaqueToken;
    }

    public void setOpaqueToken(UUID opaqueToken) {
        this.opaqueToken = opaqueToken;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public UUID activePractitionerRoleId() {
        return activePractitionerRoleId;
    }

    public void setActivePractitionerRoleId(UUID activePractitionerRoleId) {
        this.activePractitionerRoleId = activePractitionerRoleId;
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
