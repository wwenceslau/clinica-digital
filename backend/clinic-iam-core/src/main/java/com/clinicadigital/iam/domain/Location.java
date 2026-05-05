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
 * JPA entity for the {@code locations} table.
 *
 * Represents a physical or virtual location (FHIR R4 Location resource).
 * Used as a FK in {@link PractitionerRole}.
 *
 * Refs: FR-020, data-model.md
 */
@Entity
@Table(name = "locations")
public class Location {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "fhir_resource_id", nullable = false)
    private String fhirResourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_meta_profile", nullable = false, columnDefinition = "jsonb")
    private String fhirMetaProfile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_identifier_json", nullable = false, columnDefinition = "jsonb")
    private String fhirIdentifierJson;

    @Column(name = "fhir_name", nullable = false)
    private String fhirName;

    @Column(name = "fhir_status", nullable = false, length = 32)
    private String fhirStatus;

    @Column(name = "fhir_mode", nullable = false, length = 32)
    private String fhirMode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "account_active", nullable = false)
    private boolean accountActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Location() {
        // JPA constructor
    }

    public Location(UUID id, UUID tenantId, UUID organizationId,
                    String fhirResourceId, String fhirMetaProfile, String fhirIdentifierJson,
                    String fhirName, String fhirStatus, String fhirMode, String displayName) {
        this.id = id;
        this.tenantId = tenantId;
        this.organizationId = organizationId;
        this.fhirResourceId = fhirResourceId;
        this.fhirMetaProfile = fhirMetaProfile;
        this.fhirIdentifierJson = fhirIdentifierJson;
        this.fhirName = fhirName;
        this.fhirStatus = fhirStatus;
        this.fhirMode = fhirMode;
        this.displayName = displayName;
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
    public String getFhirResourceId() { return fhirResourceId; }
    public String getFhirName() { return fhirName; }
    public String getFhirStatus() { return fhirStatus; }
    public String getFhirMode() { return fhirMode; }
    public String getDisplayName() { return displayName; }
    public boolean isAccountActive() { return accountActive; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Location that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
