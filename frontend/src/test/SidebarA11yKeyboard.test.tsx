import { fireEvent, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Sidebar } from "../components/organisms/Sidebar";
import type { SidebarDomainSchema } from "../types/domain.types";
import { renderWithShellProviders } from "./renderWithShellProviders";

const schema: SidebarDomainSchema = {
  domains: [
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
        {
          resource_id: "access-profiles",
          label_key: "sidebar.security.access-profiles",
          icon_id: "security",
          route: "/admin/security/roles",
          domain_id: "seguranca",
          permission_key: "perm.security:read:access-profiles",
          permitted: true,
        },
      ],
    },
  ],
};

describe("Sidebar keyboard accessibility", () => {
  it("supports arrow navigation and enter activation", async () => {
    const onNavigate = vi.fn();
    renderWithShellProviders(<Sidebar schema={schema} onNavigate={onNavigate} />);
    fireEvent.click(screen.getByRole("button", { name: "Seguranca" }));

    const firstItem = await screen.findByRole("menuitem", { name: "Gestao de Usuarios Internos" });
    firstItem.focus();

    fireEvent.keyDown(firstItem, { key: "ArrowDown" });

    const secondItem = await screen.findByRole("menuitem", { name: "Perfis de Acesso" });
    expect(secondItem).toHaveFocus();

    fireEvent.keyDown(secondItem, { key: "Enter" });
    expect(onNavigate).toHaveBeenCalledWith("/admin/security/roles");
  });
});
