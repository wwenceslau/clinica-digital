package com.clinicadigital.iam.application;

import com.clinicadigital.iam.domain.IamGroup;
import com.clinicadigital.iam.domain.IamGroupRepository;
import com.clinicadigital.iam.domain.IamPermission;
import com.clinicadigital.iam.domain.IamPermissionRepository;
import com.clinicadigital.iam.domain.IamUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * T102 [US6] Application service for RBAC group and permission management.
 *
 * Operations:
 * <ul>
 *   <li>Create tenant-scoped groups</li>
 *   <li>List groups for a tenant</li>
 *   <li>List the global permission catalog</li>
 *   <li>Assign permissions to groups</li>
 *   <li>Assign users to groups</li>
 *   <li>Check whether a user has a specific permission via group membership</li>
 * </ul>
 *
 * Refs: FR-006, plan.md RBAC matrix
 */
@Service
public class RbacGroupService {

    private final IamGroupRepository groupRepository;
    private final IamPermissionRepository permissionRepository;
    private final AuditService auditService;
    private final IamUserRepository userRepository;
    private final RbacPermissionMap rbacPermissionMap;

    public RbacGroupService(IamGroupRepository groupRepository,
                            IamPermissionRepository permissionRepository,
                            AuditService auditService,
                            IamUserRepository userRepository,
                            RbacPermissionMap rbacPermissionMap) {
        this.groupRepository = groupRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.rbacPermissionMap = rbacPermissionMap;
    }

    /**
     * T105: Verifies the calling admin has the {@code iam.rbac.manage} permission
     * by looking up their profile and checking via {@link RbacPermissionMap}.
     * Throws {@link SecurityException} when access is denied.
     */
    private void requireRbacManage(UUID tenantId, UUID adminUserId) {
        var user = userRepository.findByIdAndTenantId(adminUserId, tenantId)
                .orElseThrow(() -> new SecurityException("rbac.manage.denied"));
        if (!rbacPermissionMap.hasPermission(user.getProfile(), "iam.rbac.manage")) {
            throw new SecurityException("rbac.manage.denied");
        }
    }

    /**
     * Creates a new group for the given tenant.
     *
     * @throws IllegalArgumentException if a group with the same name already exists in the tenant.
     */
    @Transactional
    public IamGroupResult createGroup(UUID tenantId, String name, String description, UUID adminUserId) {
        requireRbacManage(tenantId, adminUserId);
        if (groupRepository.existsByNameAndTenantId(name, tenantId)) {
            throw new IllegalArgumentException("group.name.conflict");
        }
        IamGroup group = new IamGroup(UUID.randomUUID(), tenantId, name, description);
        groupRepository.save(group);

        auditService.logAuthEvent(tenantId, adminUserId, "iam.group.created", "success",
                null, "{\"groupName\":\"" + name + "\"}");

        return new IamGroupResult(group.getId(), group.getTenantId(), group.getName(), group.getDescription());
    }

    /**
     * Returns all groups for the given tenant.
     */
    @Transactional(readOnly = true)
    public List<IamGroupResult> listGroups(UUID tenantId) {
        return groupRepository.findAllByTenantId(tenantId).stream()
                .map(g -> new IamGroupResult(g.getId(), g.getTenantId(), g.getName(), g.getDescription()))
                .toList();
    }

    /**
     * Returns the global permission catalog (all permissions, not tenant-scoped).
     */
    @Transactional(readOnly = true)
    public List<IamPermissionResult> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new IamPermissionResult(p.getId(), p.getCode(), p.getResource(), p.getAction(), p.getDescription()))
                .toList();
    }

    /**
     * Returns all permissions assigned to a specific group (for display in the admin UI).
     *
     * @throws NoSuchElementException if the group does not belong to the tenant.
     */
    @Transactional(readOnly = true)
    public List<IamPermissionResult> listGroupPermissions(UUID tenantId, UUID groupId) {
        groupRepository.findByIdAndTenantId(groupId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("group.not.found"));
        return groupRepository.findPermissionsByGroupId(groupId).stream()
                .map(p -> new IamPermissionResult(p.getId(), p.getCode(), p.getResource(), p.getAction(), p.getDescription()))
                .toList();
    }

    /**
     * Assigns a permission (by id) to a group.
     *
     * @throws NoSuchElementException if group or permission is not found.
     */
    @Transactional
    public void assignPermissionToGroup(UUID tenantId, UUID groupId, UUID permissionId, UUID adminUserId) {
        requireRbacManage(tenantId, adminUserId);
        groupRepository.findByIdAndTenantId(groupId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("group.not.found"));
        permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NoSuchElementException("permission.not.found"));

        groupRepository.assignPermission(groupId, permissionId);

        auditService.logAuthEvent(tenantId, adminUserId, "iam.group.permission.assigned", "success",
                null, "{\"groupId\":\"" + groupId + "\",\"permissionId\":\"" + permissionId + "\"}");
    }

    /**
     * Assigns a user to a group.
     *
     * @throws NoSuchElementException if group does not belong to the tenant.
     */
    @Transactional
    public void assignUserToGroup(UUID tenantId, UUID groupId, UUID userId, UUID adminUserId) {
        requireRbacManage(tenantId, adminUserId);
        groupRepository.findByIdAndTenantId(groupId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("group.not.found"));

        groupRepository.assignUser(userId, groupId, adminUserId);

        auditService.logAuthEvent(tenantId, adminUserId, "iam.group.user.assigned", "success",
                null, "{\"groupId\":\"" + groupId + "\",\"userId\":\"" + userId + "\"}");
    }

    /**
     * Returns all permissions accessible to a user via their group memberships.
     */
    @Transactional(readOnly = true)
    public List<IamPermissionResult> listUserPermissions(UUID tenantId, UUID userId) {
        return groupRepository.findPermissionsByUserId(userId).stream()
                .map(p -> new IamPermissionResult(p.getId(), p.getCode(), p.getResource(), p.getAction(), p.getDescription()))
                .toList();
    }

    /**
     * Returns true if the user has the specified permission code via any group membership.
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID tenantId, UUID userId, String permissionCode) {
        return groupRepository.findPermissionsByUserId(userId).stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
    }

    // ---- Result records ----

    public record IamGroupResult(UUID groupId, UUID tenantId, String name, String description) {}

    public record IamPermissionResult(UUID permissionId, String code, String resource, String action, String description) {}
}
