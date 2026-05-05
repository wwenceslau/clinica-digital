package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code practitioner_roles} table.
 *
 * Links a practitioner to an organization and location with a specific role code.
 * Used for multi-org login resolution (US4): identifies which organizations
 * a practitioner has access to via active roles.
 *
 * Refs: FR-020, data-model.md
 */
@Entity
@Table(name = "practitioner_roles")
public class PractitionerRole {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "practitioner_id", nullable = false)
    private UUID practitionerId;

    @Column(name = "fhir_resource_id", nullable = false)
    private String fhirResourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_meta_profile", nullable = false, columnDefinition = "jsonb")
    private String fhirMetaProfile;

    @Column(name = "role_code", nullable = false, length = 64)
    private String roleCode;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "primary_role", nullable = false)
    private boolean primaryRole = false;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_code_json", columnDefinition = "jsonb")
    private String fhirCodeJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_specialty_json", columnDefinition = "jsonb")
    private String fhirSpecialtyJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PractitionerRole() {
        // JPA constructor
    }

    public PractitionerRole(UUID id, UUID tenantId, UUID organizationId, UUID locationId,
                             UUID practitionerId, String fhirResourceId, String fhirMetaProfile,
                             String roleCode, boolean active, boolean primaryRole) {
        this.id = id;
        this.tenantId = tenantId;
        this.organizationId = organizationId;
        this.locationId = locationId;
        this.practitionerId = practitionerId;
        this.fhirResourceId = fhirResourceId;
        this.fhirMetaProfile = fhirMetaProfile;
        this.roleCode = roleCode;
        this.active = active;
        this.primaryRole = primaryRole;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getLocationId() { return locationId; }
    public UUID getPractitionerId() { return practitionerId; }
    public String getFhirResourceId() { return fhirResourceId; }
    public String getRoleCode() { return roleCode; }
    public boolean isActive() { return active; }
    public boolean isPrimaryRole() { return primaryRole; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PractitionerRole that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
