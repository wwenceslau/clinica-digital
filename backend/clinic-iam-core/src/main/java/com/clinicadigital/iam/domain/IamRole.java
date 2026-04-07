package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "iam_roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_iam_roles_tenant_role_key", columnNames = {"tenant_id", "role_key"})
        }
)
public class IamRole {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role_key", nullable = false)
    private String roleKey;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IamRole() {
        // JPA constructor
    }

    public IamRole(UUID id, UUID tenantId, String roleKey, String description) {
        this.id = id;
        this.tenantId = tenantId;
        this.roleKey = roleKey;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
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

    public String getRoleKey() {
        return roleKey;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IamRole iamRole)) {
            return false;
        }
        return Objects.equals(id, iamRole.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
