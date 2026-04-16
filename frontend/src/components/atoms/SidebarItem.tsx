import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import Tooltip from "@mui/material/Tooltip";
import { useTranslation } from "react-i18next";
import type { SidebarResourceItem } from "../../types/domain.types";

type SidebarItemProps = {
  item: SidebarResourceItem;
  onNavigate?: (route: string) => void;
};

export function SidebarItem({ item, onNavigate }: SidebarItemProps) {
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
      <Tooltip title={!item.permitted ? reason : ""} placement="right">
        <span style={{ width: "100%" }}>
          <ListItemButton
            disabled={!item.permitted}
            onClick={handleClick}
            role="menuitem"
            aria-disabled={!item.permitted}
            data-testid={`sidebar-resource-${item.resource_id}`}
          >
            <ListItemText primary={label} secondary={item.permitted ? undefined : reason} />
          </ListItemButton>
        </span>
      </Tooltip>
    </ListItem>
  );
}
