/**
 * T104 [US6] RbacPermissionGuard atom.
 *
 * Renders its children only when the authenticated user's profile includes
 * the required permission key. If the user is not authenticated or their
 * profile does not grant the permission, nothing is rendered.
 *
 * Profile-to-permission mapping mirrors the backend {@code RbacPermissionMap}:
 * - Profile 0  (super): iam.super.bootstrap, iam.tenant.create, iam.rbac.manage
 * - Profile 10 (admin): iam.auth.login, iam.auth.select_org, iam.context.select_location, iam.rbac.manage
 * - Profile 20 (practitioner): iam.auth.login, iam.auth.select_org, iam.context.select_location
 *
 * Refs: FR-006, RbacPermissionMap.java
 */
import type { ReactNode } from "react";
import { useAuth } from "../../context/AuthContext";
import { useTenant } from "../../context/TenantContext";

const PROFILE_PERMISSIONS: Record<number, ReadonlySet<string>> = {
  0: new Set(["iam.super.bootstrap", "iam.tenant.create", "iam.rbac.manage"]),
  10: new Set(["iam.auth.login", "iam.auth.select_org", "iam.context.select_location", "iam.rbac.manage"]),
  20: new Set(["iam.auth.login", "iam.auth.select_org", "iam.context.select_location"]),
};

interface RbacPermissionGuardProps {
  /** The permission key required to render children, e.g. {@code "iam.rbac.manage"}. */
  permission: string;
  children: ReactNode;
}

/**
 * Renders children only when the current user's profile grants {@code permission}.
 *
 * profileType is sourced from TenantContext (populated via GET /api/users/me/context)
 * which is the authoritative runtime value. Falls back to session.practitioner.profileType
 * for environments where TenantContext is not available or not yet resolved.
 */
export function RbacPermissionGuard({ permission, children }: RbacPermissionGuardProps) {
  const { isAuthenticated, session } = useAuth();
  const tenant = useTenant();

  if (!isAuthenticated || session === null) {
    return null;
  }

  // Prefer TenantContext.profileType (resolved from GET /api/users/me/context).
  // Falls back to session value, which may be -1 (placeholder) right after login.
  const profileType = tenant.profileType ?? session.practitioner.profileType;
  const allowed = PROFILE_PERMISSIONS[profileType]?.has(permission) ?? false;

  if (!allowed) {
    return null;
  }

  return <>{children}</>;
}
