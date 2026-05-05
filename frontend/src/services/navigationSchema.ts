import type { SidebarDomainSchema } from "../types/domain.types";

const NAV_SCHEMA_ENDPOINT =
  (import.meta.env.VITE_API_SHELL_NAVIGATION_SCHEMA as string | undefined) ?? "/api/shell/navigation-schema";

export async function fetchNavigationSchema(tenantId: string): Promise<SidebarDomainSchema> {
  const response = await fetch(NAV_SCHEMA_ENDPOINT, {
    headers: {
      "Content-Type": "application/json",
      "X-Tenant-ID": tenantId,
    },
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Failed to load navigation schema (${response.status}): ${body}`);
  }

  return (await response.json()) as SidebarDomainSchema;
}

export function ensureSecurityDomain(schema: SidebarDomainSchema): SidebarDomainSchema {
  const hasSecurity = schema.domains.some((domain) => domain.domain_id === "seguranca");
  if (hasSecurity) {
    return schema;
  }

  return {
    ...schema,
    domains: [
      ...schema.domains,
      {
        domain_id: "seguranca",
        domain_label_key: "sidebar.domain.security",
        icon_id: "lock",
        resources: [
          {
            resource_id: "internal-user-management",
            label_key: "sidebar.security.user-management",
            icon_id: "people",
            route: "/admin/security/users",
            domain_id: "seguranca",
            permission_key: "perm.security:read:user-management",
            permitted: false,
            disabled_reason: "Acesso restrito",
          },
          {
            resource_id: "access-profiles",
            label_key: "sidebar.security.access-profiles",
            icon_id: "verified_user",
            route: "/admin/security/roles",
            domain_id: "seguranca",
            permission_key: "perm.security:read:access-profiles",
            permitted: false,
            disabled_reason: "Acesso restrito",
          },
          {
            resource_id: "audit-trail",
            label_key: "sidebar.security.audit",
            icon_id: "history",
            route: "/admin/security/audit",
            domain_id: "seguranca",
            permission_key: "perm.security:read:audit",
            permitted: false,
            disabled_reason: "Acesso restrito",
          },
        ],
      },
    ],
  };
}

export function filterNavigationByTenant(schema: SidebarDomainSchema, tenantId: string): SidebarDomainSchema {
  if (schema.tenant_id && schema.tenant_id !== tenantId) {
    return {
      ...schema,
      tenant_id: tenantId,
      domains: [],
    };
  }

  return {
    ...schema,
    tenant_id: tenantId,
    domains: schema.domains
      .map((domain) => ({
        ...domain,
        resources: domain.resources.filter((resource) => {
          if (resource.domain_id !== domain.domain_id) {
            return false;
          }

          const scopedTenants = resource.metadata?.tenant_ids;
          if (!scopedTenants || scopedTenants.length === 0) {
            return true;
          }

          return scopedTenants.includes(tenantId);
        }),
      }))
      .filter((domain) => domain.resources.length > 0),
  };
}

export function getDefaultNavigationSchema(tenantId: string): SidebarDomainSchema {
  return {
    version: "1.0.0",
    tenant_id: tenantId,
    domains: [
      {
        domain_id: "administracao",
        domain_label_key: "sidebar.domain.administration",
        icon_id: "admin_panel_settings",
        resources: [
          {
            resource_id: "tenant-settings",
            label_key: "sidebar.admin.tenant-settings",
            icon_id: "settings",
            route: "/admin/tenants",
            domain_id: "administracao",
            permission_key: "perm.admin:read:tenant-settings",
            permitted: true,
          },
        ],
      },
      {
        domain_id: "seguranca",
        domain_label_key: "sidebar.domain.security",
        icon_id: "lock",
        resources: [
          {
            resource_id: "internal-user-management",
            label_key: "sidebar.security.user-management",
            icon_id: "people",
            route: "/admin/security/users",
            domain_id: "seguranca",
            permission_key: "perm.security:read:user-management",
            permitted: true,
          },
          {
            resource_id: "access-profiles",
            label_key: "sidebar.security.access-profiles",
            icon_id: "verified_user",
            route: "/admin/security/roles",
            domain_id: "seguranca",
            permission_key: "perm.security:read:access-profiles",
            permitted: true,
          },
          {
            resource_id: "audit-trail",
            label_key: "sidebar.security.audit",
            icon_id: "history",
            route: "/admin/security/audit",
            domain_id: "seguranca",
            permission_key: "perm.security:read:audit",
            permitted: false,
            disabled_reason: "Acesso restrito",
          },
        ],
      },
    ],
  };
}
