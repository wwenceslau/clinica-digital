package com.clinicadigital.iam.infrastructure;

import com.clinicadigital.iam.domain.IamGroup;
import com.clinicadigital.iam.domain.IamGroupRepository;
import com.clinicadigital.iam.domain.IamPermission;
import com.clinicadigital.iam.domain.IamUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T102 [US6] JPA implementation of {@link IamGroupRepository}.
 *
 * Uses raw JPQL and native queries for join-table operations. No Spring Data JPA.
 *
 * Refs: FR-006, data-model.md
 */
@Repository
class IamGroupRepositoryJpa implements IamGroupRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public IamGroup save(IamGroup group) {
        if (entityManager.find(IamGroup.class, group.getId()) == null) {
            entityManager.persist(group);
            return group;
        }
        return entityManager.merge(group);
    }

    @Override
    public Optional<IamGroup> findByIdAndTenantId(UUID id, UUID tenantId) {
        return entityManager.createQuery(
                        "SELECT g FROM IamGroup g WHERE g.id = :id AND g.tenantId = :tenantId",
                        IamGroup.class)
                .setParameter("id", id)
                .setParameter("tenantId", tenantId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<IamGroup> findAllByTenantId(UUID tenantId) {
        return entityManager.createQuery(
                        "SELECT g FROM IamGroup g WHERE g.tenantId = :tenantId ORDER BY g.name",
                        IamGroup.class)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }

    @Override
    public boolean existsByNameAndTenantId(String name, UUID tenantId) {
        List<IamGroup> result = entityManager.createQuery(
                        "SELECT g FROM IamGroup g WHERE lower(g.name) = lower(:name) AND g.tenantId = :tenantId",
                        IamGroup.class)
                .setParameter("name", name)
                .setParameter("tenantId", tenantId)
                .setMaxResults(1)
                .getResultList();
        return !result.isEmpty();
    }

    @Override
    @Transactional
    public void assignPermission(UUID groupId, UUID permissionId) {
        entityManager.createNativeQuery(
                        "INSERT INTO iam_group_permissions (group_id, permission_id) VALUES (?, ?) " +
                        "ON CONFLICT (group_id, permission_id) DO NOTHING")
                .setParameter(1, groupId)
                .setParameter(2, permissionId)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void removePermission(UUID groupId, UUID permissionId) {
        entityManager.createNativeQuery(
                        "DELETE FROM iam_group_permissions WHERE group_id = ? AND permission_id = ?")
                .setParameter(1, groupId)
                .setParameter(2, permissionId)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void assignUser(UUID userId, UUID groupId, UUID assignedBy) {
        entityManager.createNativeQuery(
                        "INSERT INTO iam_user_groups (iam_user_id, group_id, assigned_at, assigned_by_user_id) " +
                        "VALUES (?, ?, ?, ?) ON CONFLICT (iam_user_id, group_id) DO NOTHING")
                .setParameter(1, userId)
                .setParameter(2, groupId)
                .setParameter(3, Instant.now())
                .setParameter(4, assignedBy)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void removeUser(UUID userId, UUID groupId) {
        entityManager.createNativeQuery(
                        "DELETE FROM iam_user_groups WHERE iam_user_id = ? AND group_id = ?")
                .setParameter(1, userId)
                .setParameter(2, groupId)
                .executeUpdate();
    }

    @Override
    public List<IamPermission> findPermissionsByGroupId(UUID groupId) {
        return entityManager.createQuery(
                        "SELECT p FROM IamPermission p " +
                        "WHERE p.id IN (SELECT gp.permissionId FROM IamGroupPermission gp WHERE gp.groupId = :groupId)",
                        IamPermission.class)
                .setParameter("groupId", groupId)
                .getResultList();
    }

    @Override
    public List<IamPermission> findPermissionsByUserId(UUID userId) {
        return entityManager.createQuery(
                        "SELECT DISTINCT p FROM IamPermission p " +
                        "WHERE p.id IN (" +
                        "  SELECT gp.permissionId FROM IamGroupPermission gp " +
                        "  WHERE gp.groupId IN (" +
                        "    SELECT ug.groupId FROM IamUserGroup ug WHERE ug.userId = :userId" +
                        "  )" +
                        ")",
                        IamPermission.class)
                .setParameter("userId", userId)
                .getResultList();
    }

            @Override
            public List<IamUser> findUsersByGroupId(UUID groupId) {
            return entityManager.createQuery(
                    "SELECT u FROM IamUser u " +
                    "WHERE u.id IN (" +
                    "  SELECT ug.userId FROM IamUserGroup ug WHERE ug.groupId = :groupId" +
                    ") ORDER BY u.createdAt",
                    IamUser.class)
                .setParameter("groupId", groupId)
                .getResultList();
            }

            @Override
            @Transactional
            public boolean deleteByIdAndTenantId(UUID groupId, UUID tenantId) {
            entityManager.createNativeQuery("DELETE FROM iam_user_groups WHERE group_id = ?")
                .setParameter(1, groupId)
                .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM iam_group_permissions WHERE group_id = ?")
                .setParameter(1, groupId)
                .executeUpdate();

            int deletedGroups = entityManager.createNativeQuery(
                    "DELETE FROM iam_groups WHERE id = ? AND tenant_id = ?")
                .setParameter(1, groupId)
                .setParameter(2, tenantId)
                .executeUpdate();
            return deletedGroups > 0;
            }
}
