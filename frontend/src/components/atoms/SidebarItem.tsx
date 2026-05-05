import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import Tooltip from "@mui/material/Tooltip";
import { useTranslation } from "react-i18next";
import type { SidebarResourceItem } from "../../types/domain.types";
import { NavIcon } from "./NavIcon";

type SidebarItemProps = {
  item: SidebarResourceItem;
  onNavigate?: (route: string) => void;
  collapsed?: boolean;
};

export function SidebarItem({ item, onNavigate, collapsed = false }: SidebarItemProps) {
  const { t } = useTranslation();
  const label = t(item.label_key);
  const reason = item.disabled_reason ?? t("a11y.permission-restricted");

  const handleClick = () => {
    if (!item.permitted) {
      return;
    }
    onNavigate?.(item.route);
  };

  return (
    <ListItem disablePadding>
      <Tooltip
        title={!item.permitted ? reason : collapsed ? label : ""}
        placement="right"
        arrow={collapsed}
      >
        <span style={{ width: "100%" }}>
          <ListItemButton
            disabled={!item.permitted}
            onClick={handleClick}
            role="menuitem"
            aria-disabled={!item.permitted}
            aria-label={label}
            data-testid={`sidebar-resource-${item.resource_id}`}
            sx={{
              justifyContent: collapsed ? "center" : "flex-start",
              px: collapsed ? 1.5 : 2,
              minHeight: 44,
              gap: 1.5,
            }}
          >
            <NavIcon
              iconId={item.icon_id}
              fontSize="small"
              sx={{ flexShrink: 0, color: item.permitted ? "inherit" : "action.disabled" }}
            />
            {!collapsed && (
              <ListItemText
                primary={label}
                secondary={item.permitted ? undefined : reason}
                primaryTypographyProps={{
                  noWrap: false,
                  sx: { whiteSpace: "normal", overflowWrap: "anywhere", lineHeight: 1.25 },
                }}
                secondaryTypographyProps={{
                  sx: { whiteSpace: "normal", overflowWrap: "anywhere", lineHeight: 1.2 },
                }}
              />
            )}
          </ListItemButton>
        </span>
      </Tooltip>
    </ListItem>
  );
}
