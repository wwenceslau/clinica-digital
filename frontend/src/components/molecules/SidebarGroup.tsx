import Collapse from "@mui/material/Collapse";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import Tooltip from "@mui/material/Tooltip";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { SidebarDomainGroup } from "../../types/domain.types";
import { NavIcon } from "../atoms/NavIcon";
import { SidebarItem } from "../atoms/SidebarItem";

type SidebarGroupProps = {
  group: SidebarDomainGroup;
  onNavigate?: (route: string) => void;
  collapsed?: boolean;
};

export function SidebarGroup({ group, onNavigate, collapsed = false }: SidebarGroupProps) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);
  const domainLabel = t(group.domain_label_key);
  const domainIconId = group.icon_id ?? group.domain_id;

  if (collapsed) {
    return (
      <li>
        {group.resources.map((resource) => (
          <SidebarItem
            key={resource.resource_id}
            item={resource}
            onNavigate={onNavigate}
            collapsed
          />
        ))}
      </li>
    );
  }

  return (
    <li>
      <Tooltip title="" placement="right">
        <ListItemButton
          onClick={() => setExpanded((current) => !current)}
          aria-expanded={expanded}
          aria-controls={`sidebar-domain-${group.domain_id}`}
          data-testid={`sidebar-domain-toggle-${group.domain_id}`}
          sx={{ gap: 1.5, minHeight: 44 }}
        >
          <NavIcon
            iconId={domainIconId}
            fontSize="small"
            sx={{ flexShrink: 0, color: "text.secondary" }}
          />
          <ListItemText
            primary={domainLabel}
            primaryTypographyProps={{
              fontWeight: 600,
              noWrap: false,
              sx: { whiteSpace: "normal", overflowWrap: "anywhere", lineHeight: 1.25 },
            }}
          />
        </ListItemButton>
      </Tooltip>
      <Collapse in={expanded} timeout="auto" unmountOnExit>
        <List id={`sidebar-domain-${group.domain_id}`} disablePadding sx={{ pl: 1 }}>
          {group.resources.map((resource) => (
            <SidebarItem key={resource.resource_id} item={resource} onNavigate={onNavigate} />
          ))}
        </List>
      </Collapse>
    </li>
  );
}
