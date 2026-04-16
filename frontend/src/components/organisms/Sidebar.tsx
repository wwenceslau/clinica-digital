import Box from "@mui/material/Box";
import List from "@mui/material/List";
import { useMemo } from "react";
import type { KeyboardEvent } from "react";
import type { SidebarDomainSchema } from "../../types/domain.types";
import { SidebarGroup } from "../molecules/SidebarGroup";

type SidebarProps = {
  schema: SidebarDomainSchema;
  onNavigate?: (route: string) => void;
};

export function Sidebar({ schema, onNavigate }: SidebarProps) {
  const groups = useMemo(() => schema.domains.filter((domain) => domain.visible !== false), [schema.domains]);

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    const menuItems = Array.from(event.currentTarget.querySelectorAll<HTMLElement>("[role='menuitem']"));

    if (menuItems.length === 0) {
      return;
    }

    const activeElement = document.activeElement as HTMLElement | null;
    const currentIndex = menuItems.findIndex((item) => item === activeElement);
    if (currentIndex === -1) {
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      const next = menuItems[(currentIndex + 1) % menuItems.length];
      next?.focus();
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      const previousIndex = (currentIndex - 1 + menuItems.length) % menuItems.length;
      menuItems[previousIndex]?.focus();
      return;
    }

    if (event.key === "Enter") {
      activeElement.click();
    }
  };

  return (
    <Box component="nav" aria-label="Shell navigation" onKeyDown={handleKeyDown}>
      <List component="ul" role="menu" sx={{ p: 0, m: 0, listStyle: "none" }}>
        {groups.map((group) => (
          <SidebarGroup key={group.domain_id} group={group} onNavigate={onNavigate} />
        ))}
      </List>
    </Box>
  );
}
