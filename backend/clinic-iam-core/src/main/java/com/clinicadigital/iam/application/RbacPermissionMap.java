package com.clinicadigital.iam.application;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class RbacPermissionMap {

    private final Map<Integer, Set<String>> permissionsByProfile = Map.of(
            0, Set.of(
                    "iam.super.bootstrap",
                    "iam.tenant.create",
                    "iam.rbac.manage"
            ),
            10, Set.of(
                    "iam.auth.login",
                    "iam.auth.select_org",
                    "iam.context.select_location",
                    "iam.rbac.manage"
            ),
            20, Set.of(
                    "iam.auth.login",
                    "iam.auth.select_org",
                    "iam.context.select_location"
            )
    );

    public Set<String> permissionsForProfile(int profile) {
        return permissionsByProfile.getOrDefault(profile, Set.of());
    }

    public boolean hasPermission(int profile, String permissionKey) {
        if (permissionKey == null || permissionKey.isBlank()) {
            return false;
        }
        return permissionsForProfile(profile).contains(permissionKey);
    }
}
