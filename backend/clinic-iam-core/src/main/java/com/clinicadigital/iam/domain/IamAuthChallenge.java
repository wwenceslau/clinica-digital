package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code iam_auth_challenges} table.
 *
 * Created during multi-org login when a user has 2+ active organizations.
 * The challenge token is opaque and short-lived; it encodes which organizations
 * the user may select from. The caller exchanges it via
 * {@code POST /api/auth/select-organization}.
 *
 * Refs: US4, FR-007c
 */
@Entity
@Table(name = "iam_auth_challenges")
public class IamAuthChallenge {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "iam_user_id", nullable = false)
    private UUID iamUserId;

    /** SHA-256 digest of the opaque challenge token returned to the client. */
    @Column(name = "challenge_token_digest", nullable = false)
    private String challengeTokenDigest;

    /** JSONB array of organization option objects: [{organizationId, displayName, cnes}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "organization_options_json", nullable = false, columnDefinition = "jsonb")
    private String organizationOptionsJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IamAuthChallenge() {
        // JPA constructor
    }

    public IamAuthChallenge(UUID id, UUID iamUserId, String challengeTokenDigest,
                             String organizationOptionsJson, Instant expiresAt) {
        this.id = id;
        this.iamUserId = iamUserId;
        this.challengeTokenDigest = challengeTokenDigest;
        this.organizationOptionsJson = organizationOptionsJson;
        this.expiresAt = expiresAt;
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

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getIamUserId() { return iamUserId; }
    public String getChallengeTokenDigest() { return challengeTokenDigest; }
    public String getOrganizationOptionsJson() { return organizationOptionsJson; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IamAuthChallenge that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
