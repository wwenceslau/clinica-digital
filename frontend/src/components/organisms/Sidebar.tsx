import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import Tooltip from "@mui/material/Tooltip";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import { useMemo } from "react";
import type { KeyboardEvent } from "react";
import { useTranslation } from "react-i18next";
import type { SidebarDomainSchema } from "../../types/domain.types";
import { SidebarGroup } from "../molecules/SidebarGroup";

type SidebarProps = {
  schema: SidebarDomainSchema;
  onNavigate?: (route: string) => void;
  footer?: React.ReactNode;
  collapsed?: boolean;
  onToggleCollapsed?: () => void;
};

export function Sidebar({ schema, onNavigate, footer, collapsed = false, onToggleCollapsed }: SidebarProps) {
  const { t } = useTranslation();
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
      activeElement?.click();
    }
  };

  return (
    <Box
      component="nav"
      aria-label="Shell navigation"
      onKeyDown={handleKeyDown}
      sx={{ display: "flex", flexDirection: "column", height: "100%", overflow: "hidden" }}
    >
      {/* Expand / Collapse toggle */}
      <Box
        sx={{
          display: "flex",
          justifyContent: collapsed ? "center" : "flex-end",
          borderBottom: "1px solid",
          borderColor: "divider",
          p: 0.5,
        }}
      >
        <Tooltip title={collapsed ? t("sidebar.expand") : t("sidebar.collapse")} placement="right">
          <IconButton
            size="small"
            onClick={onToggleCollapsed}
            aria-label={collapsed ? t("sidebar.expand") : t("sidebar.collapse")}
            data-testid="sidebar-toggle"
          >
            {collapsed ? <ChevronRightIcon fontSize="small" /> : <ChevronLeftIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
      </Box>

      {/* Navigation list — hidden when collapsed */}
      {!collapsed && (
        <List component="ul" sx={{ p: 0, m: 0, listStyle: "none", flexGrow: 1, overflowY: "auto" }}>
          {groups.map((group) => (
            <SidebarGroup key={group.domain_id} group={group} onNavigate={onNavigate} />
          ))}
        </List>
      )}

      {/* Collapsed mode: show icon-only items for each resource */}
      {collapsed && (
        <List component="ul" sx={{ p: 0, m: 0, listStyle: "none", flexGrow: 1, overflowY: "auto" }}>
          {groups.map((group) => (
            <SidebarGroup key={group.domain_id} group={group} onNavigate={onNavigate} collapsed />
          ))}
        </List>
      )}

      {/* Footer: user info + logout */}
      {footer && (
        <Box
          sx={{
            borderTop: "1px solid",
            borderColor: "divider",
            p: collapsed ? 0.5 : 1.5,
            overflow: "hidden",
          }}
        >
          {footer}
        </Box>
      )}
    </Box>
  );
}
