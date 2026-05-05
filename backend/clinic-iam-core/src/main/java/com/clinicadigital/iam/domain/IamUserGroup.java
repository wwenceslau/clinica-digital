package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * T102 [US6] Join entity for iam_user_groups table (user ↔ group m2m).
 *
 * Refs: FR-006, data-model.md, V200 migration
 */
@Entity
@Table(name = "iam_user_groups")
public class IamUserGroup {

    @EmbeddedId
    private IamUserGroupId id;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by_user_id")
    private UUID assignedByUserId;

    protected IamUserGroup() {
        // JPA constructor
    }

    public IamUserGroup(UUID userId, UUID groupId, UUID assignedByUserId) {
        this.id = new IamUserGroupId(userId, groupId);
        this.assignedAt = Instant.now();
        this.assignedByUserId = assignedByUserId;
    }

    public UUID getUserId() { return id.userId; }
    public UUID getGroupId() { return id.groupId; }
    public Instant getAssignedAt() { return assignedAt; }
    public UUID getAssignedByUserId() { return assignedByUserId; }

    @Embeddable
    public static class IamUserGroupId implements Serializable {

        @Column(name = "iam_user_id", nullable = false, updatable = false)
        private UUID userId;

        @Column(name = "group_id", nullable = false, updatable = false)
        private UUID groupId;

        protected IamUserGroupId() {}

        public IamUserGroupId(UUID userId, UUID groupId) {
            this.userId = userId;
            this.groupId = groupId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IamUserGroupId that)) return false;
            return Objects.equals(userId, that.userId) &&
                   Objects.equals(groupId, that.groupId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, groupId);
        }
    }
}
