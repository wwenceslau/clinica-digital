package com.clinicadigital.iam.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T102 [US6] Repository for {@link IamGroup}.
 *
 * Refs: FR-006
 */
public interface IamGroupRepository {

    IamGroup save(IamGroup group);

    Optional<IamGroup> findByIdAndTenantId(UUID id, UUID tenantId);

    List<IamGroup> findAllByTenantId(UUID tenantId);

    boolean existsByNameAndTenantId(String name, UUID tenantId);

    /**
     * Assigns a permission to a group by inserting into {@code iam_group_permissions}.
     * No-op if the association already exists.
     */
    void assignPermission(UUID groupId, UUID permissionId);

    /**
     * Removes a permission from a group.
     * No-op if the association does not exist.
     */
    void removePermission(UUID groupId, UUID permissionId);

    /**
     * Assigns a user to a group by inserting into {@code iam_user_groups}.
     * No-op if the association already exists.
     *
     * @param userId      the iam_user id
     * @param groupId     the iam_group id
     * @param assignedBy  the admin user performing the assignment
     */
    void assignUser(UUID userId, UUID groupId, UUID assignedBy);

    /**
     * Removes a user from a group.
     * No-op if the association does not exist.
     */
    void removeUser(UUID userId, UUID groupId);

    /**
     * Returns all permissions assigned to a group.
     */
    List<IamPermission> findPermissionsByGroupId(UUID groupId);

    /**
     * Returns all permissions accessible to a user (union of all groups the user belongs to).
     */
    List<IamPermission> findPermissionsByUserId(UUID userId);

    /**
     * Returns all users assigned to a group.
     */
    List<IamUser> findUsersByGroupId(UUID groupId);

    /**
     * Deletes a group in tenant scope.
     *
     * @return true when the group existed and was deleted.
     */
    boolean deleteByIdAndTenantId(UUID groupId, UUID tenantId);
}
