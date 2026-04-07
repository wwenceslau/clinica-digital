package com.clinicadigital.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "plan_tier", nullable = false)
    private String planTier;

    @Column(name = "quota_requests_per_minute", nullable = false)
    private Integer quotaRequestsPerMinute;

    @Column(name = "quota_concurrency", nullable = false)
    private Integer quotaConcurrency;

    @Column(name = "quota_storage_mb", nullable = false)
    private Integer quotaStorageMb;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Tenant() {
        // JPA constructor
    }

    public static Tenant newTenant(String slug, String legalName, String planTier) {
        Tenant tenant = new Tenant();
        tenant.id = UUID.randomUUID();
        tenant.slug = requireText(slug, "slug");
        tenant.legalName = requireText(legalName, "legalName");
        tenant.planTier = requireText(planTier, "planTier");
        tenant.status = "active";
        tenant.quotaRequestsPerMinute = 60;
        tenant.quotaConcurrency = 10;
        tenant.quotaStorageMb = 1024;
        return tenant;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getStatus() {
        return status;
    }

    public String getPlanTier() {
        return planTier;
    }

    public Integer getQuotaRequestsPerMinute() {
        return quotaRequestsPerMinute;
    }

    public Integer getQuotaConcurrency() {
        return quotaConcurrency;
    }

    public Integer getQuotaStorageMb() {
        return quotaStorageMb;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateQuota(Integer requestsPerMinute, Integer concurrency, Integer storageMb) {
        if (requestsPerMinute != null) {
            this.quotaRequestsPerMinute = requirePositive(requestsPerMinute, "quotaRequestsPerMinute");
        }
        if (concurrency != null) {
            this.quotaConcurrency = requirePositive(concurrency, "quotaConcurrency");
        }
        if (storageMb != null) {
            this.quotaStorageMb = requirePositive(storageMb, "quotaStorageMb");
        }
    }

    public void block() {
        this.status = "blocked";
    }

    public void unblock() {
        this.status = "active";
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static int requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Tenant tenant)) {
            return false;
        }
        return Objects.equals(id, tenant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
