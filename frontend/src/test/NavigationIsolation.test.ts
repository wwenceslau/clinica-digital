import { describe, expect, it } from "vitest";
import { filterNavigationByTenant } from "../services/navigationSchema";
import type { SidebarDomainSchema } from "../types/domain.types";

describe("navigation tenant isolation", () => {
  it("removes resources not scoped to current tenant", () => {
    const schema: SidebarDomainSchema = {
      tenant_id: "tenant-clinica-a",
      domains: [
        {
          domain_id: "administracao",
          domain_label_key: "sidebar.domain.administration",
          resources: [
            {
              resource_id: "a-settings",
              label_key: "sidebar.admin.tenant-settings",
              icon_id: "settings",
              route: "/admin/a",
              domain_id: "administracao",
              permission_key: "perm.admin:read:a",
              permitted: true,
              metadata: { tenant_ids: ["tenant-clinica-a"] },
            },
            {
              resource_id: "b-settings",
              label_key: "sidebar.admin.tenant-settings",
              icon_id: "settings",
              route: "/admin/b",
              domain_id: "administracao",
              permission_key: "perm.admin:read:b",
              permitted: true,
              metadata: { tenant_ids: ["tenant-clinica-b"] },
            },
          ],
        },
      ],
    };

    const filtered = filterNavigationByTenant(schema, "tenant-clinica-a");

    expect(filtered.domains).toHaveLength(1);
    expect(filtered.domains[0].resources).toHaveLength(1);
    expect(filtered.domains[0].resources[0].resource_id).toBe("a-settings");
  });

  it("returns empty schema when top-level tenant does not match", () => {
    const schema: SidebarDomainSchema = {
      tenant_id: "tenant-x",
      domains: [
        {
          domain_id: "administracao",
          domain_label_key: "sidebar.domain.administration",
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
      ],
    };

    const filtered = filterNavigationByTenant(schema, "tenant-y");

    expect(filtered.tenant_id).toBe("tenant-y");
    expect(filtered.domains).toHaveLength(0);
  });
});
