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
 * JPA entity for the {@code practitioners} table.
 *
 * <p>Represents the clinical/professional identity in the FHIR domain.
 * Not the canonical source for authentication — {@link IamUser} owns credentials.
 *
 * <p>FHIR R4 Practitioner profile: {@code BRProfissionalSaude}
 * ({@code http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude})
 *
 * <p>{@code tenant_id} is nullable: super-user (profile 0) has a global
 * practitioner with no tenant affiliation.
 *
 * Refs: FR-001, FR-017, FR-020
 */
@Entity
@Table(name = "practitioners")
public class Practitioner {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Null for global super-user practitioner; filled for tenant-scoped profiles. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "fhir_resource_id", nullable = false)
    private String fhirResourceId;

    /** JSONB array — must contain at least one profile URI (e.g. BRProfissionalSaude). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_meta_profile", nullable = false, columnDefinition = "jsonb")
    private String fhirMetaProfile;

    /** JSONB array — sanitized FHIR Identifier[]; must never store CPF in plaintext. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_identifier_json", nullable = false, columnDefinition = "jsonb")
    private String fhirIdentifierJson;

    /** JSONB array — FHIR HumanName[]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_name_json", nullable = false, columnDefinition = "jsonb")
    private String fhirNameJson;

    @Column(name = "fhir_active", nullable = false)
    private boolean fhirActive = true;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "account_active", nullable = false)
    private boolean accountActive = true;

    /** AES-PGP–encrypted CPF bytes. Null for the system super-user practitioner. */
    @Column(name = "cpf_encrypted")
    private byte[] cpfEncrypted;

    /** Encryption key version used to produce {@link #cpfEncrypted}. */
    @Column(name = "encryption_key_version", length = 32)
    private String encryptionKeyVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Practitioner() {
        // JPA constructor
    }

    /** Constructor for tenant-admin practitioner (with encrypted CPF). */
    public Practitioner(UUID id, UUID tenantId, String fhirResourceId,
                        String fhirMetaProfile, String fhirIdentifierJson,
                        String fhirNameJson, String displayName,
                        byte[] cpfEncrypted, String encryptionKeyVersion) {
        this(id, tenantId, fhirResourceId, fhirMetaProfile, fhirIdentifierJson,
                fhirNameJson, displayName);
        this.cpfEncrypted = cpfEncrypted;
        this.encryptionKeyVersion = encryptionKeyVersion;
    }

    public Practitioner(UUID id, UUID tenantId, String fhirResourceId,
                        String fhirMetaProfile, String fhirIdentifierJson,
                        String fhirNameJson, String displayName) {
        this.id = id;
        this.tenantId = tenantId;
        this.fhirResourceId = fhirResourceId;
        this.fhirMetaProfile = fhirMetaProfile;
        this.fhirIdentifierJson = fhirIdentifierJson;
        this.fhirNameJson = fhirNameJson;
        this.displayName = displayName;
        this.fhirActive = true;
        this.accountActive = true;
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

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
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

    public String getFhirNameJson() {
        return fhirNameJson;
    }

    public boolean isFhirActive() {
        return fhirActive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAccountActive() {
        return accountActive;
    }

    public byte[] getCpfEncrypted() {
        return cpfEncrypted;
    }

    public String getEncryptionKeyVersion() {
        return encryptionKeyVersion;
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
        if (!(other instanceof Practitioner p)) return false;
        return Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
