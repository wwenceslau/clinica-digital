package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "iam_users")
public class IamUser {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_algo", nullable = false)
    private String passwordAlgo = "bcrypt";

    @Column(name = "account_active", nullable = false)
    private boolean active;

    /** Profile: 0 = super-user, 10 = tenant admin, 20 = practitioner. Default 20. */
    @Column(name = "profile", nullable = false)
    private int profile = 20;

    /** FK to practitioners(id); nullable for users without a practitioner record. */
    @Column(name = "practitioner_id")
    private UUID practitionerId;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IamUser() {
        // JPA constructor
    }

    public IamUser(UUID id, UUID tenantId, String username, String email, String passwordHash, boolean active, Instant lastLoginAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.lastLoginAt = lastLoginAt;
    }

    public IamUser(UUID id, UUID tenantId, String username, String email,
                   String passwordHash, String passwordAlgo, boolean active,
                   int profile, UUID practitionerId) {
        this.id = id;
        this.tenantId = tenantId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordAlgo = passwordAlgo;
        this.active = active;
        this.profile = profile;
        this.practitionerId = practitionerId;
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

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordAlgo() {
        return passwordAlgo;
    }

    public boolean isActive() {
        return active;
    }

    public int getProfile() {
        return profile;
    }

    public UUID getPractitionerId() {
        return practitionerId;
    }

    public void setPractitionerId(UUID practitionerId) {
        this.practitionerId = practitionerId;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IamUser iamUser)) {
            return false;
        }
        return Objects.equals(id, iamUser.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
