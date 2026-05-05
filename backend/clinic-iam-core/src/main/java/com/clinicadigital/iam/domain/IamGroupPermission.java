package com.clinicadigital.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * T102 [US6] Join entity for iam_group_permissions table (group ↔ permission m2m).
 *
 * Refs: FR-006, data-model.md, V203 migration
 */
@Entity
@Table(name = "iam_group_permissions")
public class IamGroupPermission {

    @EmbeddedId
    private IamGroupPermissionId id;

    protected IamGroupPermission() {
        // JPA constructor
    }

    public IamGroupPermission(UUID groupId, UUID permissionId) {
        this.id = new IamGroupPermissionId(groupId, permissionId);
    }

    public UUID getGroupId() { return id.groupId; }
    public UUID getPermissionId() { return id.permissionId; }

    @Embeddable
    public static class IamGroupPermissionId implements Serializable {

        @Column(name = "group_id", nullable = false, updatable = false)
        private UUID groupId;

        @Column(name = "permission_id", nullable = false, updatable = false)
        private UUID permissionId;

        protected IamGroupPermissionId() {}

        public IamGroupPermissionId(UUID groupId, UUID permissionId) {
            this.groupId = groupId;
            this.permissionId = permissionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IamGroupPermissionId that)) return false;
            return Objects.equals(groupId, that.groupId) &&
                   Objects.equals(permissionId, that.permissionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, permissionId);
        }
    }
}
