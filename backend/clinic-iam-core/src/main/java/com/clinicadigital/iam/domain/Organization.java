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
 * JPA entity for the {@code organizations} table.
 *
 * Maps to: clinic-gateway-app/src/main/resources/db/migration/V200__*.sql
 * FHIR profile: BREstabelecimentoSaude
 */
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "cnes", nullable = false, length = 7)
    private String cnes;

    @Column(name = "display_name", nullable = false)
    private String displayName;

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

    @Column(name = "fhir_active", nullable = false)
    private boolean fhirActive = true;

    @Column(name = "quota_tier", nullable = false)
    private String quotaTier = "standard";

    @Column(name = "account_active", nullable = false)
    private boolean accountActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Organization() {
    }

    /**
     * Create a new Organization with FHIR fields.
     *
     * @param id                 Unique identifier (separate from tenant_id)
     * @param tenantId           Owning tenant
     * @param cnes               7-digit CNES code (must be globally unique)
     * @param displayName        Human-readable organisation name (unique per tenant)
     * @param fhirResourceId     FHIR resource logical id
     * @param fhirMetaProfile    JSONB string with RNDS profile URL array
     * @param fhirIdentifierJson JSONB string with FHIR identifier array (CNES)
     * @param fhirName           Plain-text name for FHIR name element
     */
    public Organization(
            UUID id,
            UUID tenantId,
            String cnes,
            String displayName,
            String fhirResourceId,
            String fhirMetaProfile,
            String fhirIdentifierJson,
            String fhirName) {
        this.id = id;
        this.tenantId = tenantId;
        this.cnes = cnes;
        this.displayName = displayName;
        this.fhirResourceId = fhirResourceId;
        this.fhirMetaProfile = fhirMetaProfile;
        this.fhirIdentifierJson = fhirIdentifierJson;
        this.fhirName = fhirName;
        this.fhirActive = true;
        this.quotaTier = "standard";
        this.accountActive = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCnes() {
        return cnes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFhirResourceId() {
        return fhirResourceId;
    }

    public String getFhirMetaProfile() {
        return fhirMetaProfile;
    }

    public String getFhirIdentifierJson() {
        return fhirIdentifierJson;
    }

    public String getFhirName() {
        return fhirName;
    }

    public boolean isFhirActive() {
        return fhirActive;
    }

    public String getQuotaTier() {
        return quotaTier;
    }

    public boolean isAccountActive() {
        return accountActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Organization o)) return false;
        return Objects.equals(id, o.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
