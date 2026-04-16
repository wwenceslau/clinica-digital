import { fireEvent, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
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

describe("Sidebar permissions", () => {
  it("renders restricted resources as disabled and shows tooltip reason", async () => {
    renderWithShellProviders(<Sidebar schema={schema} />);
    fireEvent.click(screen.getByRole("button", { name: "Seguranca" }));

    const item = await screen.findByTestId("sidebar-resource-audit-trail");
    expect(item).toHaveAttribute("aria-disabled", "true");

    fireEvent.mouseOver(item);
    expect(await screen.findByText("Acesso restrito")).toBeInTheDocument();
  });
});
