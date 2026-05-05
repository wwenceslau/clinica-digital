package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * T102 [US6] IAM Group entity.
 *
 * Maps to {@code iam_groups} table. Groups are tenant-scoped: same name
 * can exist across tenants, but must be unique within a tenant (DB unique
 * index on {@code (tenant_id, lower(name))}).
 *
 * Refs: FR-006, data-model.md
 */
@Entity
@Table(name = "iam_groups")
public class IamGroup {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IamGroup() {
        // JPA constructor
    }

    public IamGroup(UUID id, UUID tenantId, String name, String description) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
    }

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
