import { fireEvent, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Sidebar } from "../components/organisms/Sidebar";
import type { SidebarDomainSchema } from "../types/domain.types";
import { renderWithShellProviders } from "./renderWithShellProviders";

const schema: SidebarDomainSchema = {
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
    {
      domain_id: "seguranca",
      domain_label_key: "sidebar.domain.security",
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
      ],
    },
  ],
};

describe("Sidebar", () => {
  it("renders domain groups and resource items", async () => {
    renderWithShellProviders(<Sidebar schema={schema} />);

    expect(screen.getByRole("button", { name: "Administracao" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Seguranca" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Administracao" }));
    fireEvent.click(screen.getByRole("button", { name: "Seguranca" }));

    expect(await screen.findByRole("menuitem", { name: "Configuracoes do tenant" })).toBeInTheDocument();
    expect(await screen.findByRole("menuitem", { name: "Gestao de Usuarios Internos" })).toBeInTheDocument();
  });
});
