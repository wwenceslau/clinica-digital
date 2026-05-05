package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * T070 [US9] — Append-only audit event entity.
 *
 * <p>Immutability is enforced at multiple layers:
 * <ol>
 *   <li>{@link Immutable} — Hibernate never generates UPDATE SQL for this entity.</li>
 *   <li>All {@code @Column} definitions carry {@code updatable = false}.</li>
 *   <li>{@link IamAuditEventRepository#save} delegates to {@code EntityManager.persist()},
 *       which rejects detached / already-managed instances — no MERGE path.</li>
 *   <li>DB-level: {@code iam_audit_events} RLS policy allows only INSERT (no UPDATE/DELETE
 *       for application roles).</li>
 * </ol>
 *
 * <p>Required event types covered (FR-016, FR-024):
 * <ul>
 *   <li>{@code auth.login} — success and failure paths in {@code AuthenticationService}</li>
 *   <li>{@code SUPER_USER_BOOTSTRAPPED} — success and failure in {@code BootstrapSuperUserService}</li>
 *   <li>{@code TENANT_ADMIN_CREATED} — in {@code CreateTenantAdminService}</li>
 *   <li>{@code auth.select_org} — in {@code AuthenticationService}</li>
 *   <li>{@code auth.logout} — in {@code AuthenticationService}</li>
 * </ul>
 *
 * Refs: FR-016, FR-024
 */
@Entity
@Immutable
@Table(name = "iam_audit_events")
public class IamAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "outcome", nullable = false, updatable = false)
    private String outcome;

    @Column(name = "trace_id", updatable = false)
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb", updatable = false)
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IamAuditEvent() {
        // JPA constructor
    }

    public IamAuditEvent(UUID tenantId, UUID actorUserId, String eventType, String outcome, String traceId, String metadataJson) {
        this.tenantId = tenantId;
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.outcome = outcome;
        this.traceId = traceId;
        this.metadataJson = metadataJson;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
